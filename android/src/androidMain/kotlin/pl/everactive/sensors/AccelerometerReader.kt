package pl.everactive.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class AccelerometerReader(
    private val context: Context,
    private val onSensorValuesUpdated: (x: Float, y: Float, z: Float) -> Unit
) : SensorEventListener {

    // TODO: store current and previous data for anomaly detection, check in onSensorChanged
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    companion object {
        private const val TAG = "AccelerometerReader"
    }

    fun startListening() {
        if (accelerometer == null) {
            Log.e(TAG, "ACCELEROMETER NOT AVAILABLE")
            return
        }

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d(TAG, "ACCELEROMETER LISTENER START")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "ACCELEROMETER LISTENER STOP")
    }

    fun onAnomalyDetected() {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            // check for anomalies
            onSensorValuesUpdated(x, y, z)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Accuracy changed for ${sensor?.name}: $accuracy")
    }
}
