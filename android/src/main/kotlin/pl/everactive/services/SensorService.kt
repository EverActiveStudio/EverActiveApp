package pl.everactive.services

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
import java.util.Collections
import kotlin.math.pow
import kotlin.math.sqrt

class SensorService : Service(), SensorEventListener {

    // Inject AlertManager to trigger SOS flow centrally
    private val alertManager: AlertManager by inject()

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
    private val CHANNEL_ID = "SafetyMonitorChannel"

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
            // Wait for data collection window
            delay(CONFIRMATION_WINDOW_MS)

            // Analyze collected data
            analyzePostFallData()

            // Reset flags for next event
            isVerifyingFall = false
            postFallDataBuffer.clear()
        }
    }

    private suspend fun analyzePostFallData() {
        // Copy data to local list to avoid concurrency issues during calculation
        val dataSnapshot = postFallDataBuffer.toList()

        if (dataSnapshot.isEmpty()) return

        // Calculate average (approx. 9.81 m/s^2 if stationary)
        val average = dataSnapshot.average()

        // Calculate Standard Deviation to check device stability
        var variance = 0.0
        for (value in dataSnapshot) {
            variance += (value - average).pow(2)
        }
        variance /= dataSnapshot.size
        val stdDev = sqrt(variance)

        println("Post-fall analysis: Avg=${average.format(2)}, StdDev=${stdDev.format(2)}")

        // Decision logic:
        if (stdDev < INACTIVITY_THRESHOLD) {
            // Low deviation implies inactivity -> Confirmed Man-Down
            triggerAlarm()
        } else {
            // High deviation implies movement -> False alarm
            println("Alarm cancelled: Movement detected.")
            updateNotification("Alarm cancelled. Movement detected.")
            delay(3000)
            updateNotification("Monitoring active")
        }
    }

    private suspend fun triggerAlarm() {
        println("ALARM! MAN-DOWN CONFIRMED")

        // Delegate alarm handling to AlertManager.
        withContext(Dispatchers.Main) {
            alertManager.triggerSOS()
        }

        updateNotification("Fall detected! Sending alert...")
    }

    // --- Notification & System Section ---

    private fun startForegroundService() {
        val manager = getSystemService(NotificationManager::class.java)

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safety Monitor",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
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
