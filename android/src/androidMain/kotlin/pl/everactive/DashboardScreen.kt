package pl.everactive

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class AlertStatus {
    NONE,
    PENDING,
    SENT
}

@Composable
fun DashboardScreen(
    username: String,
    onLogoutClick: () -> Unit
) {
    var alertStatus by remember { mutableStateOf(AlertStatus.NONE) }
    var pendingTimeRemaining by remember { mutableIntStateOf(5) }

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
                        text = "Status: Idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
