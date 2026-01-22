package pl.everactive

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("welcome") }
    var loggedInUser by remember { mutableStateOf("") }
    val context = LocalContext.current

    when (currentScreen) {
        "welcome" -> {
            WelcomeScreen(
                onStartShiftClick = {
                    currentScreen = "login"
                }
            )
        }
        "login" -> {
            LoginScreen(
                onLoginSuccess = { username ->
                    loggedInUser = username
                    Toast.makeText(context, "Logged in as $username", Toast.LENGTH_SHORT).show()
                    currentScreen = "dashboard"
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
                onRegisterSuccess = { email ->
                    Toast.makeText(
                        context,
                        "Account created for $email",
                        Toast.LENGTH_LONG
                    ).show()

                    currentScreen = "login"
                },
                onBackToLoginClick = {
                    currentScreen = "login"
                }
            )
        }

        "dashboard" -> {
            DashboardScreen(
                username = loggedInUser,
                onLogoutClick = {
                    currentScreen = "welcome"
                    loggedInUser = ""
                }
            )
        }
    }
}
