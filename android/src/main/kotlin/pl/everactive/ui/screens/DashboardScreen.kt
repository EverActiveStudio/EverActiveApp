package pl.everactive

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.services.ServiceController
import pl.everactive.utils.PermissionUtils

enum class AlertStatus {
    NONE,
    PENDING,
    SENT,
}

@Composable
fun DashboardScreen(
    username: String,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val serviceController: ServiceController = koinInject()
    val apiClient: EveractiveApiClient = koinInject()

    var isShiftActive by remember { mutableStateOf(false) }
    var currentShiftMillis by remember { mutableLongStateOf(0L) }
    val shiftDurationSeconds = currentShiftMillis / 1000

    var alertStatus by remember { mutableStateOf(AlertStatus.NONE) }
    var pendingTimeRemaining by remember { mutableIntStateOf(5) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            serviceController.startMonitoringService(apiClient)
        } else {
            Toast.makeText(context, "Permissions required for safety monitoring", Toast.LENGTH_SHORT).show()
        }
    }

    val continuousEasing = remember { CubicBezierEasing(0.5f, 0.2f, 0.5f, 0.8f) }

    // Handle shift active state - request permissions and start service
    LaunchedEffect(isShiftActive) {
        if (isShiftActive) {
            if (PermissionUtils.allPermissionsGranted(context)) {
                serviceController.startMonitoringService(apiClient)
            } else {
                // Request permissions
                permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
            }

            val startTime = System.currentTimeMillis()
            while (isShiftActive) {
                currentShiftMillis = System.currentTimeMillis() - startTime
                delay(16)
            }
        } else {
            currentShiftMillis = 0L
            serviceController.stopMonitoringService()
        }
    }

    LaunchedEffect(alertStatus) {
        if (alertStatus == AlertStatus.PENDING) {
            pendingTimeRemaining = 5
            while (pendingTimeRemaining > 0 && alertStatus == AlertStatus.PENDING) {
                delay(1000)
                pendingTimeRemaining--
            }
            if (alertStatus == AlertStatus.PENDING) {
                alertStatus = AlertStatus.SENT
            }
        }
    }

    // Stop service when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (isShiftActive) {
                serviceController.stopMonitoringService()
            }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome, $username",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isShiftActive) "MONITORING ACTIVE (Background Service)" else "Status: Idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isShiftActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                        onButtonClick = { alertStatus = AlertStatus.NONE }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { pendingTimeRemaining / 5f },
                                modifier = Modifier.size(64.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 6.dp
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
                        onButtonClick = { alertStatus = AlertStatus.NONE }
                    ) {
                        Text(
                            text = "Supervisor notified.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                if (isShiftActive) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val elapsedMillis = currentShiftMillis
                        val strokeWidth = 3.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2

                        val linearProgress = (elapsedMillis % 1000) / 1000f
                        val easedProgress = continuousEasing.transform(linearProgress)

                        val rotationDegrees = easedProgress * 360f
                        val finalRotation = rotationDegrees - 90f

                        val growth = (elapsedMillis / 1000f).coerceIn(0f, 1f)
                        val sweepLimit = if (elapsedMillis < 1000L) rotationDegrees else 360f
                        val sweepAngle = (360f * growth).coerceAtMost(sweepLimit)

                        val startAngle = 360f - sweepAngle

                        withTransform({
                            rotate(degrees = finalRotation, pivot = center)
                        }) {
                            val radarBrush = Brush.sweepGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.5f to primaryColor.copy(alpha = 0.1f),
                                    0.8f to primaryColor.copy(alpha = 0.5f),
                                    1.0f to primaryColor
                                )
                            )

                            drawArc(
                                brush = radarBrush,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShiftActive)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else
                                Color.Transparent
                        )
                        .border(
                            width = if (isShiftActive) 0.dp else 1.5.dp,
                            color = if (isShiftActive) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha=0.5f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isShiftActive = !isShiftActive
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isShiftActive) {
                            Text(
                                text = "ON DUTY",
                                style = MaterialTheme.typography.labelMedium,
                                color = primaryColor,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDuration(shiftDurationSeconds),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to finish",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "START SHIFT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (alertStatus == AlertStatus.NONE) {
                        alertStatus = AlertStatus.PENDING
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sos,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CALL FOR HELP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
