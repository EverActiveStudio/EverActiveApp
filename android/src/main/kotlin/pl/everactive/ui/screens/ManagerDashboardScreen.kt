package pl.everactive.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pl.everactive.clients.EveractiveApiClient
import pl.everactive.shared.UserDataDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    onLogoutClick: () -> Unit
) {
    val apiClient: EveractiveApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var users by remember { mutableStateOf<List<UserDataDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                users = apiClient.managerGetAllUserData()
            } catch (e: Exception) {
                Toast.makeText(context, "Data fetching error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manager's Panel") },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(users) { user ->
                        UserStatusCard(user)
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatusCard(user: UserDataDto) {
    val isDanger = user.state.isSos || user.state.fellRecently
    val cardColor = if (isDanger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nagłówek: Imię i Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isDanger) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "OK",
                        tint = Color(0xFF4CAF50), // Zielony
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Szczegóły statusu
            if (user.state.isSos) {
                StatusBadge("SOS ACTIVE", MaterialTheme.colorScheme.error)
            }
            if (user.state.fellRecently) {
                StatusBadge("FALL DETECTED", MaterialTheme.colorScheme.error)
            }
            if (!isDanger) {
                StatusBadge("Status: OK", Color(0xFF4CAF50))
            }

            // Ostatnia lokalizacja
            user.state.lastLocation?.let { loc ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lat: ${loc.latitude}, Lon: ${loc.longitude}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Historia alertów (Rule Status)
            if (user.ruleStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ALERT HISTORY",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                user.ruleStatus.sortedByDescending { it.timestamp }.take(5).forEach { ruleStatus ->
                    val date = remember(ruleStatus.timestamp) {
                        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(ruleStatus.timestamp))
                    }
                    val ruleName = ruleStatus.rule::class.simpleName ?: "Rule"
                    val violationColor = if (ruleStatus.isViolated) MaterialTheme.colorScheme.error else Color.Gray

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = date, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (ruleStatus.isViolated) "$ruleName (Violation)" else "$ruleName (OK)",
                            style = MaterialTheme.typography.bodySmall,
                            color = violationColor,
                            fontWeight = if(ruleStatus.isViolated) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
