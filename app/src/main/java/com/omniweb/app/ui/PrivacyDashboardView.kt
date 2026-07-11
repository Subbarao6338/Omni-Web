package com.omniweb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardView(
    url: String,
    blockedTrackers: List<String>,
    onBack: () -> Unit
) {
    val isSecure = url.startsWith("https://")
    val host = android.net.Uri.parse(url).host ?: "Local"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSecure) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isSecure) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                if (isSecure) "Secure Connection" else "Insecure Connection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isSecure) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                if (isSecure) "Your data is encrypted as it travels to $host" else "Data sent to $host might be visible to others",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text("Blocked on this page", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }

            if (blockedTrackers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No trackers detected yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val grouped = blockedTrackers.groupBy {
                    it.substringBefore(" ").removeSurrounding("[", "]")
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrivacyStat(
                            "Ads",
                            grouped["Ad"]?.size ?: 0,
                            Icons.Default.AdsClick,
                            Color(0xFFEF4444),
                            Modifier.weight(1f)
                        )
                        PrivacyStat(
                            "Analytics",
                            grouped["Analytics"]?.size ?: 0,
                            Icons.Default.Analytics,
                            Color(0xFF3B82F6),
                            Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrivacyStat(
                            "Social",
                            grouped["Social"]?.size ?: 0,
                            Icons.Default.Group,
                            Color(0xFF8B5CF6),
                            Modifier.weight(1f)
                        )
                        PrivacyStat(
                            "Malware",
                            grouped["Malware"]?.size ?: 0,
                            Icons.Default.BugReport,
                            Color(0xFFF59E0B),
                            Modifier.weight(1f)
                        )
                    }
                }

                items(blockedTrackers) { tracker ->
                    val category = tracker.substringBefore(" ")
                    val domain = tracker.substringAfter(" ")
                    ListItem(
                        headlineContent = { Text(domain) },
                        supportingContent = { Text(category) },
                        leadingContent = {
                            val color = when (category) {
                                "[Ad]" -> Color(0xFFEF4444)
                                "[Analytics]" -> Color(0xFF3B82F6)
                                "[Social]" -> Color(0xFF8B5CF6)
                                "[Malware]" -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyStat(label: String, count: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(count.toString(), fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
