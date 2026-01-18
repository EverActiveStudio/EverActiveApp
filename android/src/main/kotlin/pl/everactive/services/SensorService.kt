package pl.everactive.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import pl.everactive.MainActivity
import pl.everactive.R
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.shared.EventDto
import kotlin.math.abs
import kotlin.math.sqrt

class SensorService : Service(), SensorEventListener {

    private val apiClient: EveractiveApiClient by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager

    private var lastMovementTime = System.currentTimeMillis()
    private var stepsCounter = 0
    private var isServiceRunning = false

    // Configuration
    private val REPORTING_INTERVAL_MS = 10_000L // Send data every 10 seconds
    private val MOVEMENT_THRESHOLD = 12.0f // Threshold for movement detection (m/s^2)
    private val FALL_THRESHOLD = 25.0f // Threshold for fall detection (approx 2.5g)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            startSensorMonitoring()
            startDataReporting()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "SafetyMonitorChannel"
        val channelName = "Safety Monitoring Service"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("EverActive Guard")
            .setContentText("Monitoring your safety active...")
            .setSmallIcon(R.mipmap.ic_launcher_round) // Ensure this resource exists
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        // ID > 0 required
        startForeground(1001, notification)
    }

    private fun startSensorMonitoring() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startDataReporting() {
        serviceScope.launch {
            while (isServiceRunning) {
                delay(REPORTING_INTERVAL_MS)
                sendBatchedEvents()
            }
        }
    }

    private suspend fun sendBatchedEvents() {
        if (stepsCounter > 0) {
            val events = listOf(
                EventDto.Move(
                    timestamp = System.currentTimeMillis(),
                    steps = stepsCounter
                )
            )

            // Reset counter before sending to avoid double counting if call fails briefly
            // In production, you might want better transaction handling
            val currentSteps = stepsCounter
            stepsCounter = 0

            val result = apiClient.pushEvents(events)
            if (result != null) {
                // Restore steps if failed (simple retry logic)
                stepsCounter += currentSteps
                println("Failed to send events: ${result.message}")
            } else {
                println("Events sent successfully. Steps: $currentSteps")
            }
        } else {
            // Send a ping if no movement to show connectivity
            apiClient.pushEvents(
                listOf(EventDto.Ping(timestamp = System.currentTimeMillis()))
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Calculate total acceleration vector
                val acceleration = sqrt(x * x + y * y + z * z)

                // Detect simple movement
                if (abs(acceleration - SensorManager.GRAVITY_EARTH) > 2.0) {
                    // It's a rough approximation of a "step" or significant activity
                    stepsCounter++
                    lastMovementTime = System.currentTimeMillis()
                }

                // Detect potential fall (High impact)
                if (acceleration > FALL_THRESHOLD) {
                    handleFallDetection()
                }
            }
        }
    }

    private fun handleFallDetection() {
        println("POTENTIAL FALL DETECTED!")
        serviceScope.launch {
            // Immediately send critical data (in real app: trigger alarm logic)
            apiClient.pushEvents(
                listOf(EventDto.Ping(timestamp = System.currentTimeMillis())) // Or special Fall event if added to DTO
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }
}
