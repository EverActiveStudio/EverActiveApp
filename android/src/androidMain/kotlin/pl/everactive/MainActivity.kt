package pl.everactive

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EverActiveTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("login") }
    val context = LocalContext.current

    when (currentScreen) {
        "login" -> {
            LoginScreen(
                onLoginSuccess = { username ->
                    Toast.makeText(context, "Logged in as $username", Toast.LENGTH_SHORT).show()
                },
                onBackClick = {
                    currentScreen = "welcome"
                },
                onRegisterClick = {
                    currentScreen = "register"
                }
            )
        }
        "register" -> {
            RegisterScreen(
                onRegisterSuccess = { username, supervisor ->
                    Toast.makeText(
                        context,
                        "Account created for $username\nAlerts sent to: $supervisor",
                        Toast.LENGTH_LONG
                    ).show()

                    currentScreen = "login"
                },
                onBackToLoginClick = {
                    currentScreen = "login"
                }
            )
        }
    }
}
