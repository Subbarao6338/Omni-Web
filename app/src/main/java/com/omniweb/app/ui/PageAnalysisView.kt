package com.omniweb.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniweb.app.util.AnalysisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageAnalysisView(
    result: AnalysisResult,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                ScoreCard("SEO", result.seoScore, Icons.Default.Search, MaterialTheme.colorScheme.primary)
            }
            if (result.seoIssues.isNotEmpty()) {
                items(result.seoIssues) { issue ->
                    IssueItem(issue)
                }
            }

            item {
                ScoreCard("Accessibility", result.accessibilityScore, Icons.Default.Accessibility, Color(0xFF2E7D32))
            }
            if (result.accessibilityIssues.isNotEmpty()) {
                items(result.accessibilityIssues) { issue ->
                    IssueItem(issue)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricItem("Word Count", result.wordCount.toString())
                        MetricItem("Readability", result.readabilityScore)
                        result.performanceMetrics.forEach { (k, v) ->
                            MetricItem(k, v)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreCard(title: String, score: Int, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Score: $score/100", fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f) )
            CircularProgressIndicator(
                progress = { score.toFloat() / 100f },
                modifier = Modifier.size(48.dp),
                color = color,
                strokeWidth = 4.dp
            )
        }
    }
}

@Composable
fun IssueItem(issue: String) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(issue, fontSize = 14.sp)
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
