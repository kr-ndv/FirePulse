package com.firepulse.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.data.remote.ApiClient
import com.firepulse.app.data.remote.FireSummaryResponse
import com.firepulse.app.data.remote.FireClustersResponse
import com.firepulse.app.data.remote.HistoricalFireSummaryResponse
import com.firepulse.app.ui.components.FirePulseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InsightsScreen() {
    var summary by remember { mutableStateOf<FireSummaryResponse?>(null) }
    var clusters by remember { mutableStateOf<FireClustersResponse?>(null) }
    var clusterError by remember { mutableStateOf<String?>(null) }
    var historical by remember { mutableStateOf<HistoricalFireSummaryResponse?>(null) }
    var historicalDate by remember { mutableStateOf("2026-10-01") }
    var historicalLoading by remember { mutableStateOf(false) }
    var historicalError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        summary = null
        clusters = null
        clusterError = null
        try {
            summary = withContext(Dispatchers.IO) {
                ApiClient.firePulseApi.getSummary(68.0, 6.0, 98.0, 37.5, 5, "VIIRS_SNPP_NRT")
            }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unable to load analytics"
        }
        try {
            clusters = withContext(Dispatchers.IO) {
                ApiClient.firePulseApi.getClusters(68.0, 6.0, 98.0, 37.5, 5, "VIIRS_SNPP_NRT", 5.0)
            }
        } catch (clusterException: Exception) {
            clusterError = clusterException.localizedMessage ?: "Unable to load hotspot clusters"
        }
        loading = false
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Insights", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Refresh") }
        }
        Text("India · VIIRS SNPP · Latest 5 days", color = MaterialTheme.colorScheme.onSurfaceVariant)
        summary?.let { data ->
            if (data.data_status == "stale_cache") {
                Text(
                    "NASA FIRMS is temporarily unavailable. Showing the server snapshot captured at " +
                        (data.cached_at ?: "an earlier time") + ".",
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Text("Live API response", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text("Analytics unavailable: $it", color = MaterialTheme.colorScheme.error) }

        clusters?.let { result ->
            FirePulseCard(Modifier.fillMaxWidth()) {
                Text("Potential fire-activity clusters", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${result.cluster_count} clusters from ${result.detection_count} detections · " +
                        "${result.radius_km} km grouping radius",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Heuristic score, not an official danger rating",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                result.clusters.take(8).forEach { cluster ->
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Cluster ${cluster.cluster_id} · ${cluster.severity_label.uppercase()}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${cluster.detection_count} detections · ${cluster.high_confidence_count} high confidence",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${cluster.latitude}, ${cluster.longitude}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column {
                            Text("${cluster.severity_score}/100", style = MaterialTheme.typography.titleMedium)
                            Text("${cluster.frp_total_mw} MW", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(result.scoring_method, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        clusterError?.let {
            Text("Cluster analysis unavailable: $it", color = MaterialTheme.colorScheme.error)
        }

        FirePulseCard(Modifier.fillMaxWidth()) {
            Text("Historical activity", style = MaterialTheme.typography.titleMedium)
            Text(
                "Query an archived date window from NASA FIRMS. Standard Processing products may be delayed compared with near-real-time feeds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = historicalDate,
                onValueChange = { historicalDate = it },
                label = { Text("Start date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    historicalLoading = true
                    historicalError = null
                    historical = null
                    val validDate = runCatching { java.time.LocalDate.parse(historicalDate) }.isSuccess
                    if (!validDate) {
                        historicalError = "Enter a valid date in YYYY-MM-DD format."
                        historicalLoading = false
                    } else {
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                historical = withContext(Dispatchers.IO) {
                                    ApiClient.firePulseApi.getHistoricalSummary(
                                        68.0, 6.0, 98.0, 37.5, historicalDate, 5, "VIIRS_SNPP_SP"
                                    )
                                }
                            } catch (e: Exception) {
                                historicalError = e.localizedMessage ?: "Historical data unavailable"
                            } finally {
                                historicalLoading = false
                            }
                        }
                    }
                },
                enabled = !historicalLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (historicalLoading) "Loading archive…" else "Load 5-day archive") }
            historicalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            historical?.let { archive ->
                HorizontalDivider()
                Text(archive.start_date + " to " + archive.end_date + " · " + archive.source,
                    style = MaterialTheme.typography.labelMedium)
                Text(archive.count.toString() + " detections", style = MaterialTheme.typography.headlineMedium)
                Text(archive.high_confidence_count.toString() + " high-confidence · " + archive.frp_total_mw + " MW FRP",
                    style = MaterialTheme.typography.bodyMedium)
                Text(archive.note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        summary?.let { data ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FirePulseCard(Modifier.weight(1f)) {
                    Text("Detections", style = MaterialTheme.typography.labelMedium)
                    Text(data.count.toString(), style = MaterialTheme.typography.headlineMedium)
                }
                FirePulseCard(Modifier.weight(1f)) {
                    Text("High confidence", style = MaterialTheme.typography.labelMedium)
                    Text(data.high_confidence_count.toString(), style = MaterialTheme.typography.headlineMedium)
                }
            }
            FirePulseCard(Modifier.fillMaxWidth()) {
                Text("Total fire radiative power", style = MaterialTheme.typography.titleMedium)
                Text(data.frp_total_mw.toString() + " MW", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Sum of reported FRP values in this request; not a measure of burned area.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FirePulseCard(Modifier.fillMaxWidth()) {
                Text("Daily detections", style = MaterialTheme.typography.titleMedium)
                val maxValue = (data.by_day.maxOfOrNull { it.detections } ?: 1).coerceAtLeast(1)
                data.by_day.forEach { day ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(day.date, style = MaterialTheme.typography.bodySmall)
                        Text(day.detections.toString(), style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(
                        progress = day.detections.toFloat() / maxValue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            FirePulseCard(Modifier.fillMaxWidth()) {
                Text("Satellite distribution", style = MaterialTheme.typography.titleMedium)
                data.by_satellite.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.satellite)
                        Text(item.detections.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Text("Retrieved: " + data.retrieved_on, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (summary == null && !loading && error == null) {
            Text("No analytics are available for this time window.")
        }
    }
}
