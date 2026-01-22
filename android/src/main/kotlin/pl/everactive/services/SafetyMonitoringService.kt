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
        private const val LOCATION_UPDATE_INTERVAL_MS = 60000L // 1 minuta
        private const val EVENT_BATCH_SIZE = 10
        private const val EVENT_SEND_INTERVAL_MS = 5000L
    }

    private val binder = ServiceBinder()
    private var isMonitoring = false

    // Sensory
    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    // Lokalizacja
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null

    // Kroki
    private var stepCount = 0
    private var lastStepCount = 0

    // Wykrywanie Upadku
    private val FALL_THRESHOLD = 15.0f // Próg 1.5G
    private val IMMOBILITY_THRESHOLD = 3.5f
    private val FALL_ANALYSIS_WINDOW_MS = 5_000L

    private var potentialFallDetected = false
    private val recentAccelerationData = ArrayDeque<Float>(50)

    // Zmienna Anty-Spamowa (Cooldown)
    private var lastShockTime = 0L

    // Zależności
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // LISTA ZDARZEŃ - musi być chroniona!
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

        // Bezpieczna inicjalizacja
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            apiClient = get()
            alertManager = get()

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
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelfResult(startId)
            }
        }
        return START_NOT_STICKY // Zmiana na NOT_STICKY, żeby system nie restartował serwisu bez intencji
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
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stepDetectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                0f,
                this,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Ignorujemy brak uprawnień
        }

        startEventSync()
        recordEvent(EventDto.Ping(timestamp = System.currentTimeMillis()))
    }

    private fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false

        // Wyrejestrowanie sensorów
        try {
            sensorManager.unregisterListener(this)
            locationManager.removeUpdates(this)
        } catch (e: Exception) { e.printStackTrace() }

        analysisJob?.cancel()
        releaseWakeLock()

        // Anulowanie pętli synchronizacji
        scope.coroutineContext.cancelChildren()

        // BEZPIECZNE WYSŁANIE OSTATNICH DANYCH (Fix dla ConcurrentModificationException)
        GlobalScope.launch {
            val finalEvents = mutableListOf<EventDto>()

            // Kopiujemy dane w bloku synchronized
            synchronized(eventList) {
                if (stepCount > lastStepCount) {
                    eventList.add(EventDto.Move(timestamp = System.currentTimeMillis(), steps = stepCount - lastStepCount))
                    lastStepCount = stepCount
                }
                finalEvents.addAll(eventList)
                eventList.clear()
            }

            // Wysyłamy kopię (już bez blokady synchronized)
            if (finalEvents.isNotEmpty()) {
                try {
                    apiClient.pushEvents(finalEvents)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

                    // Logika Upadku z Cooldownem
                    if (acceleration > FALL_THRESHOLD && !potentialFallDetected) {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastShockTime > 3000) {
                            lastShockTime = currentTime
                            handlePotentialFall()
                        }
                    }

                    // Buforowanie
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
            delay(FALL_ANALYSIS_WINDOW_MS)

            // Sprawdzenie czy użytkownik wciąż leży
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
        // Wszystko w try-catch, aby błąd UI/Alertu nie ubił serwisu
        try {
            // 1. Logika biznesowa
            alertManager.triggerSOS()

            // 2. Powiadomienie
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("ALARM UPADKU")
                .setContentText("Procedura ratunkowa uruchomiona.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(ALERT_NOTIFICATION_ID, notification)

            // 3. Debug Toast (opcjonalnie)
            Toast.makeText(applicationContext, "Wykryto upadek!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireWakeLock() {
        try {
            wakeLock?.takeIf { !it.isHeld }?.acquire(FALL_ANALYSIS_WINDOW_MS + 2000)
        } catch (e: Exception) { /* ignore */ }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) { /* ignore */ }
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

        // 1. Kopiujemy dane szybko w bloku synchronized
        synchronized(eventList) {
            if (eventList.isEmpty()) return
            eventsToSend = eventList.take(EVENT_BATCH_SIZE).toMutableList()
        }

        // 2. Wysyłamy (wolno, poza blokadą)
        try {
            apiClient.pushEvents(eventsToSend)

            // 3. Usuwamy wysłane (szybko w bloku synchronized)
            synchronized(eventList) {
                eventList.removeAll(eventsToSend)
            }
        } catch (e: Exception) {
            // Błąd sieci - dane zostają
        }
    }

    private fun recordEvent(event: EventDto) {
        synchronized(eventList) {
            eventList.add(event)
        }
    }

    // --- BOILERPLATE ---
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onLocationChanged(location: Location) {
        if (!isMonitoring) return
        lastLocation = location
        recordEvent(EventDto.Location(timestamp = System.currentTimeMillis(), latitude = location.latitude, longitude = location.longitude))
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
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
