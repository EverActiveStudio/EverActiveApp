package pl.everactive

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pl.everactive.clients.EveractiveApiToken
import pl.everactive.services.ServiceController
import pl.everactive.ui.screens.ManagerDashboardScreen

@Composable
fun AppNavigation() {
    val apiToken: EveractiveApiToken = koinInject()
    val serviceController: ServiceController = koinInject()
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val token = apiToken.get()
        if (!token.isNullOrBlank()) {
            // SPRAWDZANIE ROLI
            val role = apiToken.getRole()
            if (role == EveractiveApiToken.Role.Manager) {
                currentScreen = "manager_dashboard"
            } else {
                currentScreen = "dashboard"
            }
        } else {
            currentScreen = "welcome"
        }
    }

    if (currentScreen == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
                onLoginSuccess = { email ->
                    Toast.makeText(context, "Zalogowano jako $email", Toast.LENGTH_SHORT).show()
                    // Po zalogowaniu ponownie sprawdzamy rolę, aby przekierować w dobre miejsce
                    scope.launch {
                        val role = apiToken.getRole()
//                        if (role == EveractiveApiToken.Role.Manager) {
//                            currentScreen = "manager_dashboard"
//                        } else {
//                            currentScreen = "dashboard"
//                        }
                        currentScreen = "manager_dashboard"
                    }
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
                        "Konto utworzone dla $email",
                        Toast.LENGTH_LONG
                    ).show()
                    // Po rejestracji domyślnie user (nie menadżer), więc dashboard
                    currentScreen = "dashboard"
                },
                onBackToLoginClick = {
                    currentScreen = "login"
                }
            )
        }

        "dashboard" -> {
            DashboardScreen(
                onLogoutClick = {
                    scope.launch {
                        serviceController.stopMonitoringService()
                        apiToken.clear()
                        currentScreen = "welcome"
                        Toast.makeText(context, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // NOWY EKRAN DLA MENADŻERA
        "manager_dashboard" -> {
            ManagerDashboardScreen(
                onLogoutClick = {
                    scope.launch {
                        // Menadżer raczej nie używa serwisu monitoringu, ale dla pewności zatrzymujemy
                        serviceController.stopMonitoringService()
                        apiToken.clear()
                        currentScreen = "welcome"
                        Toast.makeText(context, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
