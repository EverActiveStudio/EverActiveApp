package pl.everactive.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import pl.everactive.MainActivity
import pl.everactive.R
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.shared.EventDto
import kotlin.math.pow
import kotlin.math.sqrt

class SafetyMonitoringService : Service(), SensorEventListener, LocationListener, KoinComponent {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "safety_monitoring_channel"
        private const val STEP_DETECTOR_UPDATE_INTERVAL_MS = 1000L
        private const val LOCATION_UPDATE_INTERVAL_MS = 60000L // 1 minute
        private const val EVENT_BATCH_SIZE = 10
        private const val EVENT_SEND_INTERVAL_MS = 30000L // 30 seconds
    }

    private val binder = ServiceBinder()
    private var isMonitoring = false

    // Sensors and Location
    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null

    // Step tracking
    private var stepCount = 0
    private var lastStepCount = 0
    private var accelerationValues = FloatArray(3)

    // Event collection
    private val eventList = mutableListOf<EventDto>()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var apiClient: EveractiveApiClient

    inner class ServiceBinder : Binder() {
        fun getService(): SafetyMonitoringService = this@SafetyMonitoringService
    }

    override fun onCreate() {
        super.onCreate()
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        apiClient = get()  // Get from Koin

        initializeSensors()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelfResult(startId)
            }
        }
        return START_STICKY
    }

    private fun initializeSensors() {
        // Try Step Detector first (most accurate)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // Fallback to Step Counter
        if (stepDetectorSensor == null) {
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        }

        // Always use accelerometer as backup for step detection
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true

        // Start foreground notification
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Register sensor listeners
        stepDetectorSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        accelerometerSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Request location updates
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                0f,
                this,
                Looper.getMainLooper()
            )
            // Fallback to network provider
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                0f,
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission denial
        }

        // Start periodic event syncing
        startEventSync()

        // Send initial ping
        recordEvent(EventDto.Ping(timestamp = System.currentTimeMillis()))
    }

    private fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false

        // Unregister sensors
        sensorManager.unregisterListener(this)

        // Stop location updates
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            // Handle permission denial
        }

        // Cancel pending syncs
        scope.coroutineContext.cancelChildren()

        // Send final event
        if (stepCount > lastStepCount) {
            recordEvent(EventDto.Move(timestamp = System.currentTimeMillis(), steps = stepCount - lastStepCount))
            lastStepCount = stepCount
        }

        // Sync remaining events
        scope.launch {
            sendPendingEvents()
        }
    }

    private fun startEventSync() {
        scope.launch {
            while (isMonitoring) {
                delay(EVENT_SEND_INTERVAL_MS)
                if (eventList.isNotEmpty()) {
                    sendPendingEvents()
                }
            }
        }
    }

    private suspend fun sendPendingEvents() {
        if (eventList.isEmpty()) return

        try {
            val eventsToSend = eventList.take(EVENT_BATCH_SIZE).toMutableList()

            apiClient.pushEvents(eventsToSend)?.let { error ->
                // Error occurred, keep events in queue for retry
            } ?: run {
                // No error means success
                eventList.removeAll(eventsToSend.toSet())
            }
        } catch (e: Exception) {
            // Keep events in queue for retry
        }
    }

    private fun recordEvent(event: EventDto) {
        synchronized(eventList) {
            eventList.add(event)
            if (eventList.size >= EVENT_BATCH_SIZE) {
                scope.launch {
                    sendPendingEvents()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isMonitoring) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                stepCount++
                recordEvent(EventDto.Move(timestamp = System.currentTimeMillis(), steps = 1))
            }

            Sensor.TYPE_STEP_COUNTER -> {
                val newSteps = event.values[0].toInt() - lastStepCount
                if (newSteps > 0) {
                    stepCount = event.values[0].toInt()
                    recordEvent(EventDto.Move(timestamp = System.currentTimeMillis(), steps = newSteps))
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                accelerationValues[0] = event.values[0]
                accelerationValues[1] = event.values[1]
                accelerationValues[2] = event.values[2]

                // Detect significant motion for additional step counting
                val acceleration = sqrt(
                    accelerationValues[0].pow(2) +
                            accelerationValues[1].pow(2) +
                            accelerationValues[2].pow(2)
                )

                // Trigger ping periodically to show activity
                if (acceleration > 15) {
                    // User is moving, periodic ping
                    scope.launch {
                        recordEvent(EventDto.Ping(timestamp = System.currentTimeMillis()))
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    override fun onLocationChanged(location: Location) {
        if (!isMonitoring) return

        lastLocation = location
        recordEvent(
            EventDto.Location(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude
            )
        )
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Safety Monitoring Active")
            .setContentText("Tracking location and activity data")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safety Monitoring",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Monitors employee safety and location"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        scope.cancel()
    }
}
