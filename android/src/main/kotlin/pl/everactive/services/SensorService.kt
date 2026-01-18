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
import java.util.Collections
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SensorService : Service(), SensorEventListener {

    private val apiClient: EveractiveApiClient by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var sensorManager: SensorManager

    // Threshold configuration
    private val FALL_THRESHOLD = 25.0f      // ~2.5g (Initial impact)
    private val INACTIVITY_THRESHOLD = 1.5f // Standard Deviation threshold (Motionless)
    private val CONFIRMATION_WINDOW_MS = 10_000L // Post-fall analysis duration (10s)

    // Service state
    private var isServiceRunning = false
    private var isVerifyingFall = false

    // Data analysis buffers
    // Synchronized list for thread-safe access (Sensor thread vs Coroutine)
    private val postFallDataBuffer: MutableList<Float> = Collections.synchronizedList(mutableListOf())

    // UI/Notification helpers
    private var notificationBuilder: NotificationCompat.Builder? = null
    private val NOTIFICATION_ID = 1001

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
        }
        return START_STICKY
    }

    private fun startSensorMonitoring() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            // SENSOR_DELAY_GAME provides higher frequency for accurate fall detection
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Calculate total acceleration vector
                val currentAcceleration = sqrt(x * x + y * y + z * z)

                if (isVerifyingFall) {
                    // Verification phase: collect data for analysis
                    postFallDataBuffer.add(currentAcceleration)
                } else {
                    // Monitoring phase: detect impact peak
                    if (currentAcceleration > FALL_THRESHOLD) {
                        handlePotentialFallDetected()
                    }
                }
            }
        }
    }

    private fun handlePotentialFallDetected() {
        isVerifyingFall = true
        postFallDataBuffer.clear()

        println("Impact detected! verifying inactivity...")
        updateNotification("Impact detected! Verifying...")

        serviceScope.launch {
            // Wait for data collection
            delay(CONFIRMATION_WINDOW_MS)

            // Analyze collected data
            analyzePostFallData()

            // Reset flags
            isVerifyingFall = false
            postFallDataBuffer.clear()
        }
    }

    private suspend fun analyzePostFallData() {
        // Copy data to avoid concurrency issues
        val dataSnapshot = postFallDataBuffer.toList()

        if (dataSnapshot.isEmpty()) return

        // Calculate average (approx. 9.81 m/s^2 if stationary)
        val average = dataSnapshot.average()

        // Calculate Standard Deviation to check device stability
        // Low std dev = device is lying still. High std dev = movement/walking.
        var variance = 0.0
        for (value in dataSnapshot) {
            variance += (value - average).pow(2)
        }
        variance /= dataSnapshot.size
        val stdDev = sqrt(variance)

        println("Post-fall analysis: Avg=${average.format(2)}, StdDev=${stdDev.format(2)}")

        // Decision logic:
        if (stdDev < INACTIVITY_THRESHOLD) {
            // Low deviation implies inactivity -> Alarm
            triggerAlarm()
        } else {
            // High deviation implies movement -> Cancel
            println("Alarm cancelled: Movement detected.")
            updateNotification("Alarm cancelled. Movement detected.")
            delay(3000)
            updateNotification("Monitoring active")
        }
    }

    private suspend fun triggerAlarm() {
        println("ALARM! MAN-DOWN CONFIRMED")
        updateNotification("ALARM! SENDING ALERT...")

        // TODO: Use specific "Fall" event type. Using "Ping" as placeholder.
        val alarmEvent = EventDto.Ping(timestamp = System.currentTimeMillis())

        val result = apiClient.pushEvents(listOf(alarmEvent))

        if (result == null) {
            updateNotification("Alert sent to HQ!")
        } else {
            updateNotification("Error sending alert!")
        }
    }

    // --- Notification & System Section ---

    private fun startForegroundService() {
        val channelId = "SafetyMonitorChannel"
        val manager = getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Safety Monitor",
                NotificationManager.IMPORTANCE_HIGH // High for visibility
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("EverActive Guard")
            .setContentText("Monitoring active")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notificationBuilder!!.build())
    }

    private fun updateNotification(text: String) {
        notificationBuilder?.let {
            it.setContentText(text)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, it.build())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
