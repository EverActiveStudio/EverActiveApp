package pl.everactive.services

import android.Manifest
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
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import pl.everactive.MainActivity
import pl.everactive.R
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.shared.EventDto
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SafetyMonitoringService : Service(), SensorEventListener, LocationListener, KoinComponent {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "safety_monitoring_channel"
        private const val ALERT_NOTIFICATION_ID = 999
        private const val LOCATION_UPDATE_INTERVAL_MS = 60000L
        private const val EVENT_BATCH_SIZE = 10
        private const val EVENT_SEND_INTERVAL_MS = 5000L
    }

    private val binder = ServiceBinder()
    private var isMonitoring = false

    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null

    private var stepCount = 0
    private var lastStepCount = 0

    // --- LOGIKA UPADKU ---
    private var impactThreshold = 25.0f // Domyślnie ~2.5g
    private val FREE_FALL_THRESHOLD_G = 6.0f
    private val MIN_FALL_DURATION_MS = 250L
    private val MAX_IMPACT_WINDOW_MS = 1000L

    private var freeFallStartTime: Long = 0
    private var lastValidFreeFallTime: Long = 0

    // Zmienna do przechowywania siły ostatniego wykrytego uderzenia (do debugowania)
    private var lastTriggeringGForce: Float = 0.0f

    private val IMMOBILITY_THRESHOLD = 3.5f
    private val FALL_ANALYSIS_WINDOW_MS = 5_000L

    private var potentialFallDetected = false
    private val recentAccelerationData = ArrayDeque<Float>(50)
    private var lastShockTime = 0L

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val eventList = mutableListOf<EventDto>()

    private lateinit var apiClient: EveractiveApiClient
    private lateinit var alertManager: AlertManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var analysisJob: Job? = null

    inner class ServiceBinder : Binder() {
        fun getService(): SafetyMonitoringService = this@SafetyMonitoringService
    }

    override fun onCreate() {
        super.onCreate()

        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            apiClient = get()
            alertManager = get()

            val dataStoreService: DataStoreService = get()

            scope.launch {
                dataStoreService.observeSensitivity().collect { sensitivity ->
                    impactThreshold = when (sensitivity) {
                        // 1g = ~9.81 m/s^2
                        // ZWIĘKSZONE PROGI:
                        "MEDIUM" -> 80.0f // ~4.0g (Mocne uderzenie)
                        "HARD" -> 130.0f   // ~6.0g (Bardzo mocne, np. beton)
                        else -> 30.0f     // ~2.5g (SOFT - domyślne)
                    }
                }
            }

            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Eve:FallAnalysisLock")

            initializeSensors()
            createNotificationChannel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelfResult(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun initializeSensors() {
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) { e.printStackTrace() }

        stepDetectorSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL_MS, 0f, this, Looper.getMainLooper())
        } catch (e: SecurityException) { /* ignore */ }

        startEventSync()
        recordEvent(EventDto.Ping(timestamp = System.currentTimeMillis()))
    }

    private fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false

        try {
            sensorManager.unregisterListener(this)
            locationManager.removeUpdates(this)
        } catch (e: Exception) { e.printStackTrace() }

        analysisJob?.cancel()
        releaseWakeLock()
        scope.coroutineContext.cancelChildren()

        GlobalScope.launch {
            val finalEvents = mutableListOf<EventDto>()
            synchronized(eventList) {
                if (stepCount > lastStepCount) {
                    eventList.add(EventDto.Move(timestamp = System.currentTimeMillis(), steps = stepCount - lastStepCount))
                    lastStepCount = stepCount
                }
                finalEvents.addAll(eventList)
                eventList.clear()
            }
            if (finalEvents.isNotEmpty()) {
                try { apiClient.pushEvents(finalEvents) } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isMonitoring) return

        try {
            when (event.sensor.type) {
                Sensor.TYPE_STEP_DETECTOR -> {
                    stepCount++
                    recordEvent(EventDto.Move(timestamp = System.currentTimeMillis(), steps = 1))
                }

                Sensor.TYPE_STEP_COUNTER -> {
                    val valSteps = event.values[0].toInt()
                    val newSteps = valSteps - lastStepCount
                    if (newSteps > 0 && lastStepCount != 0) {
                        recordEvent(EventDto.Move(timestamp = System.currentTimeMillis(), steps = newSteps))
                    }
                    lastStepCount = valSteps
                }

                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val acceleration = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

                    // 1. FREE FALL CHECK
                    if (acceleration < FREE_FALL_THRESHOLD_G) {
                        if (freeFallStartTime == 0L) freeFallStartTime = System.currentTimeMillis()
                    } else {
                        if (freeFallStartTime != 0L) {
                            val duration = System.currentTimeMillis() - freeFallStartTime
                            if (duration >= MIN_FALL_DURATION_MS) {
                                lastValidFreeFallTime = System.currentTimeMillis()
                            }
                            freeFallStartTime = 0L
                        }
                    }

                    // 2. IMPACT CHECK
                    if (acceleration > impactThreshold && !potentialFallDetected) {
                        val currentTime = System.currentTimeMillis()
                        val timeSinceLastFall = currentTime - lastValidFreeFallTime

                        if (timeSinceLastFall < MAX_IMPACT_WINDOW_MS) {
                            if (currentTime - lastShockTime > 3000) {
                                lastShockTime = currentTime

                                // Zapisujemy siłę uderzenia w jednostkach g (9.81 m/s^2 = 1g)
                                lastTriggeringGForce = acceleration / 9.81f

                                handlePotentialFall()
                            }
                        }
                    }

                    if (potentialFallDetected) {
                        synchronized(recentAccelerationData) {
                            if (recentAccelerationData.size >= 50) recentAccelerationData.removeFirst()
                            recentAccelerationData.addLast(acceleration)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handlePotentialFall() {
        potentialFallDetected = true
        acquireWakeLock()

        analysisJob?.cancel()
        analysisJob = scope.launch(Dispatchers.Default) {
            // Czekamy, aż użytkownik się uspokoi po upadku
            delay(FALL_ANALYSIS_WINDOW_MS)

            if (isUserStill()) {
                withContext(Dispatchers.Main) {
                    triggerManDownAlert()
                }
            }

            potentialFallDetected = false
            releaseWakeLock()
        }
    }

    private fun isUserStill(): Boolean {
        synchronized(recentAccelerationData) {
            if (recentAccelerationData.isEmpty()) return true
            val average = recentAccelerationData.average()
            val maxDev = recentAccelerationData.maxOfOrNull { abs(it - average) } ?: 0.0
            return maxDev < IMMOBILITY_THRESHOLD
        }
    }

    private fun triggerManDownAlert() {
        try {
            alertManager.triggerSOS()

            // Formatujemy siłę do 1 miejsca po przecinku (np. "4.5g")
            val forceText = "Impact: %.1fg".format(lastTriggeringGForce)

            // 1. Toast dla szybkiego debugowania na ekranie
            Toast.makeText(this, "Fall Detected! $forceText", Toast.LENGTH_LONG).show()

            // 2. Powiadomienie z informacją o sile
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Fall Detected ($forceText)") // <--- TUTAJ WYŚWIETLAMY SIŁĘ
                .setContentText("Rescue procedure initiated.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(ALERT_NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireWakeLock() {
        try { wakeLock?.takeIf { !it.isHeld }?.acquire(FALL_ANALYSIS_WINDOW_MS + 2000) } catch (e: Exception) {}
    }

    private fun releaseWakeLock() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (e: Exception) {}
    }

    private fun startEventSync() {
        scope.launch {
            while (isMonitoring) {
                delay(EVENT_SEND_INTERVAL_MS)
                sendPendingEvents()
            }
        }
    }

    private suspend fun sendPendingEvents() {
        var eventsToSend: MutableList<EventDto>
        synchronized(eventList) {
            if (eventList.isEmpty()) return
            eventsToSend = eventList.take(EVENT_BATCH_SIZE).toMutableList()
        }
        try {
            apiClient.pushEvents(eventsToSend)
            synchronized(eventList) { eventList.removeAll(eventsToSend) }
        } catch (e: Exception) { }
    }

    private fun recordEvent(event: EventDto) {
        synchronized(eventList) { eventList.add(event) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onLocationChanged(location: Location) {
        if (!isMonitoring) return
        lastLocation = location
        alertManager.updateLocation(location)
        recordEvent(EventDto.Location(timestamp = System.currentTimeMillis(), latitude = location.latitude, longitude = location.longitude))
    }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EverActive Safety")
            .setContentText("Monitorowanie aktywne")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Safety Monitoring", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        scope.cancel()
    }
}
