package pl.everactive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.everactive.sensors.AccelerometerReader

class MainActivity : ComponentActivity() {

    //test code to read sensor data
    private lateinit var reader: AccelerometerReader

    private var x by mutableStateOf(0f)
    private var y by mutableStateOf(0f)
    private var z by mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reader = AccelerometerReader(this) { nx, ny, nz -> x = nx; y = ny; z = nz}

        setContent {
            MaterialTheme {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("accelerometer values", style = MaterialTheme.typography.titleMedium)
                    Text("x: ${"%.3f".format(x)}")
                    Text("y: ${"%.3f".format(y)}")
                    Text("z: ${"%.3f".format(z)}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reader.startListening()
    }

    override fun onPause() {
        reader.stopListening()
        super.onPause()
    }
}
