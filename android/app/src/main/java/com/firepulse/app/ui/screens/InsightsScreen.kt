package com.firepulse.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.data.remote.ApiClient
import com.firepulse.app.data.remote.FireSummaryResponse
import com.firepulse.app.ui.components.FirePulseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun InsightsScreen() {
 var summary by remember { mutableStateOf<FireSummaryResponse?>(null) }
 var loading by remember { mutableStateOf(true) }
 var error by remember { mutableStateOf<String?>(null) }
 var refresh by remember { mutableIntStateOf(0) }

 LaunchedEffect(refresh) {
  loading=true; error=null
  try {
   summary=withContext(Dispatchers.IO) {
    ApiClient.firePulseApi.getSummary(68.0,6.0,98.0,37.5,5,"VIIRS_SNPP_NRT")
   }
  } catch(e:Exception) {
   error=e.localizedMessage ?: "Unable to load analytics"
  } finally { loading=false }
 }

 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   Text("Insights",style=MaterialTheme.typography.headlineLarge)
   TextButton(onClick={refresh++},enabled=!loading){Text("Refresh")}
  }
  Text("India · VIIRS SNPP · Latest 5 days",color=MaterialTheme.colorScheme.onSurfaceVariant)
  summary?.let { data ->
   if(data.data_status=="stale_cache") Text("NASA FIRMS is temporarily unavailable. Showing the server snapshot captured at "+(data.cached_at ?: "an earlier time")+".",color=MaterialTheme.colorScheme.tertiary)
   else Text("Live API response",color=MaterialTheme.colorScheme.primary)
  }
  if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let { Text("Analytics unavailable: "+it,color=MaterialTheme.colorScheme.error) }
  summary?.let { data ->
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    FirePulseCard(Modifier.weight(1f)) {
     Text("Detections",style=MaterialTheme.typography.labelMedium)
     Text(data.count.toString(),style=MaterialTheme.typography.headlineMedium)
    }
    FirePulseCard(Modifier.weight(1f)) {
     Text("High confidence",style=MaterialTheme.typography.labelMedium)
     Text(data.high_confidence_count.toString(),style=MaterialTheme.typography.headlineMedium)
    }
   }
   FirePulseCard(Modifier.fillMaxWidth()) {
    Text("Total fire radiative power",style=MaterialTheme.typography.titleMedium)
    Text(data.frp_total_mw.toString()+" MW",style=MaterialTheme.typography.headlineMedium)
    Text("Sum of reported FRP values in this request; not a measure of burned area.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   FirePulseCard(Modifier.fillMaxWidth()) {
    Text("Daily detections",style=MaterialTheme.typography.titleMedium)
    val maxValue=(data.by_day.maxOfOrNull { it.detections } ?: 1).coerceAtLeast(1)
    data.by_day.forEach { day ->
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      Text(day.date,style=MaterialTheme.typography.bodySmall)
      Text(day.detections.toString(),style=MaterialTheme.typography.labelMedium)
     }
     LinearProgressIndicator(progress=day.detections.toFloat()/maxValue,modifier=Modifier.fillMaxWidth())
    }
   }
   FirePulseCard(Modifier.fillMaxWidth()) {
    Text("Satellite distribution",style=MaterialTheme.typography.titleMedium)
    data.by_satellite.forEach { item ->
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      Text(item.satellite)
      Text(item.detections.toString(),style=MaterialTheme.typography.labelLarge)
     }
    }
   }
   Text("Retrieved: "+data.retrieved_on,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  if(summary==null && !loading && error==null) Text("No analytics are available for this time window.")
 }
}
