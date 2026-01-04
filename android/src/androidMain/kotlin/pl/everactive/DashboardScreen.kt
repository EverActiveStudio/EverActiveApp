package pl.everactive

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
    var isShiftActive by remember { mutableStateOf(false) }
    var shiftDurationSeconds by remember { mutableLongStateOf(0L) }

    var alertStatus by remember { mutableStateOf(AlertStatus.NONE) }
    var pendingTimeRemaining by remember { mutableIntStateOf(5) }

    LaunchedEffect(isShiftActive) {
        if (isShiftActive) {
            val startTime = System.currentTimeMillis() - (shiftDurationSeconds * 1000)
            while (isShiftActive) {
                delay(1000)
                shiftDurationSeconds = (System.currentTimeMillis() - startTime) / 1000
            }
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
                        text = if (isShiftActive) "MONITORING ACTIVE" else "Status: Idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isShiftActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            if (!isShiftActive) {
                                shiftDurationSeconds = 0
                            }
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isShiftActive) {
                            Text(
                                text = "ON DUTY",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
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
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
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
