package pl.everactive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.services.AlertManager
import pl.everactive.services.ServiceController
import pl.everactive.utils.PermissionUtils
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch
import pl.everactive.services.DataStoreService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val serviceController: ServiceController = koinInject()
    val apiClient: EveractiveApiClient = koinInject()
    val alertManager: AlertManager = koinInject()

    val dataStoreService: DataStoreService = koinInject()
    val currentSensitivity by dataStoreService.observeSensitivity().collectAsState(initial = "SOFT")
    val scope = rememberCoroutineScope()

    var isShiftActive by remember { mutableStateOf(false) }
    var currentShiftMillis by remember { mutableLongStateOf(0L) }
    val shiftDurationSeconds = currentShiftMillis / 1000

    val alertStatus by alertManager.alertStatus.collectAsState()
    val pendingTimeRemaining by alertManager.timeRemaining.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { (_, isGranted) -> isGranted }
        if (allGranted) {
            serviceController.startMonitoringService(apiClient)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
    )

    LaunchedEffect(isShiftActive) {
        if (isShiftActive) {
            if (PermissionUtils.allPermissionsGranted(context)) {
                serviceController.startMonitoringService(apiClient)
            } else {
                permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
            }

            val startTime = System.currentTimeMillis()
            while (isShiftActive) {
                currentShiftMillis = System.currentTimeMillis() - startTime
                delay(1000)
            }
        } else {
            currentShiftMillis = 0L
            serviceController.stopMonitoringService()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isShiftActive) serviceController.stopMonitoringService()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fitInside(WindowInsetsRulers.SafeDrawing.current)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isShiftActive) "MONITORING ACTIVE" else "Status: Idle",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isShiftActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ALERT SECTION
            AnimatedVisibility(
                visible = alertStatus != AlertStatus.NONE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (alertStatus == AlertStatus.PENDING) {
                    StatusAlert(
                        title = "SENDING ALARM...",
                        buttonText = "CANCEL",
                        containerColor = Color(0xFFEF6C00),
                        onButtonClick = { alertManager.cancelSOS() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { pendingTimeRemaining / 10f },
                                modifier = Modifier.size(64.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )
                            Text(
                                text = "$pendingTimeRemaining",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    StatusAlert(
                        title = "ALARM SENT!",
                        buttonText = "CLOSE",
                        containerColor = Color(0xFFC62828),
                        onButtonClick = { alertManager.closeAlert() }
                    ) {
                        Text("Supervisor notified.", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(2.5f))

            // RADAR / BUTTON
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                if (isShiftActive) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        withTransform({ rotate(rotationAnim) }) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(Color.Transparent, primaryColor.copy(0.5f))
                                ),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(if (isShiftActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                        .border(2.dp, if(isShiftActive) primaryColor else Color.Gray, CircleShape)
                        .clickable { isShiftActive = !isShiftActive }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlayArrow, null, tint = primaryColor, modifier = Modifier.size(48.dp))
                        Text(if (isShiftActive) "STOP SHIFT" else "START SHIFT", fontWeight = FontWeight.Bold)
                        if (isShiftActive) {
                            Text(formatDuration(shiftDurationSeconds), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sensitivity Menu
            var expanded by remember { mutableStateOf(false) }
            val options = listOf(
                "SOFT" to "1.5g",
                "MEDIUM" to "2.0g",
                "HARD" to "2.5g"
            )

            val selectedOption = options.find { it.first == currentSensitivity } ?: options.first()
            val displayText = "${selectedOption.first} (${selectedOption.second})"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fall Sensitivity") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (level, gVal) ->
                        DropdownMenuItem(
                            text = { Text("$level ($gVal)") },
                            onClick = {
                                scope.launch {
                                    dataStoreService.secureSet(DataStoreService.SENSITIVITY_KEY, level)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // SOS BUTTON
            Button(
                onClick = { alertManager.triggerSOS() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Sos, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALL FOR HELP")
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
