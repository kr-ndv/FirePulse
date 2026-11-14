package com.firepulse.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.firepulse.app.data.local.FirePulseDatabase
import com.firepulse.app.data.local.toEntity
import com.firepulse.app.data.local.toObservation
import com.firepulse.app.data.remote.ApiClient
import com.firepulse.app.data.remote.FireObservation
import com.firepulse.app.ui.components.FireActivityMap
import com.firepulse.app.ui.components.FirePulseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun ExploreScreen() {
 val context=LocalContext.current
 val dao=remember { FirePulseDatabase.getInstance(context).fireObservationDao() }
 var source by remember { mutableStateOf("VIIRS_SNPP_NRT") }
 var days by remember { mutableIntStateOf(1) }
 var observations by remember { mutableStateOf<List<FireObservation>>(emptyList()) }
 var loading by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var expanded by remember { mutableStateOf(false) }
 var showingCache by remember { mutableStateOf(false) }
 var apiDataStatus by remember { mutableStateOf("live") }
 var cachedAt by remember { mutableStateOf<String?>(null) }

 LaunchedEffect(source,days) {
  loading=true; error=null; showingCache=false
  val cached=withContext(Dispatchers.IO) { dao.getSnapshot(source,days).map { it.toObservation() } }
  if(cached.isNotEmpty()) {
   observations=cached
   apiDataStatus="offline_cache"
   cachedAt=null
   showingCache=true
  }
  try {
   val response=withContext(Dispatchers.IO) {
    ApiClient.firePulseApi.getFires(68.0,6.0,98.0,37.5,days,source)
   }
   observations=response.observations
   apiDataStatus=response.data_status ?: "live"
   cachedAt=response.cached_at
   showingCache=apiDataStatus == "stale_cache"
   if(apiDataStatus=="live") {
    withContext(Dispatchers.IO) {
     val timestamp=System.currentTimeMillis()
     dao.replaceSnapshot(source,days,response.observations.map { it.toEntity(timestamp,source,days) })
     dao.deleteOlderThan(timestamp-30L*24L*60L*60L*1000L)
    }
   }
  } catch(e:Exception) {
   error=if(cached.isNotEmpty()) "Showing saved observations. Live refresh failed: "+(e.localizedMessage ?: "network unavailable")
         else e.localizedMessage ?: "Unable to reach FirePulse API"
  } finally { loading=false }
 }

 Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Explore",style=MaterialTheme.typography.headlineLarge)
  Text("Satellite-observed active fire detections across India",color=MaterialTheme.colorScheme.onSurfaceVariant)
  ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}) {
   OutlinedTextField(value=source,onValueChange={},readOnly=true,label={Text("Satellite feed")},modifier=Modifier.fillMaxWidth().menuAnchor())
   ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
    listOf("VIIRS_SNPP_NRT","VIIRS_NOAA20_NRT","VIIRS_NOAA21_NRT","MODIS_NRT").forEach { item ->
     DropdownMenuItem(text={Text(item)},onClick={source=item;expanded=false})
    }
   }
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   listOf(1,2,3,5).forEach { value ->
    FilterChip(selected=days==value,onClick={days=value},label={Text(value.toString()+" day"+(if(value>1) "s" else ""))})
   }
  }
  FirePulseCard(Modifier.fillMaxWidth()) {
   Text("India fire activity",style=MaterialTheme.typography.titleMedium)
   Text(observations.size.toString()+" satellite detections",style=MaterialTheme.typography.headlineMedium)
   Text("Feed: "+source+" · Window: "+days+" day(s)",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(apiDataStatus=="offline_cache") Text("Offline device cache",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
   else if(showingCache) Text("Recent server snapshot · NASA feed unavailable",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
   else if(!loading && error==null) Text("Live response",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
   cachedAt?.let { Text("Snapshot captured: "+it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let { Text(it,color=if(showingCache) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error) }
  FireActivityMap(observations=observations)
  Text("Map data © OpenFreeMap · © OpenStreetMap contributors",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text("Recent detections",style=MaterialTheme.typography.titleLarge)
  if(observations.isEmpty() && !loading && error==null) Text("No detections returned for this area and time window.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.weight(1f)) {
   items(observations) { fire ->
    FirePulseCard(Modifier.fillMaxWidth()) {
     Text(fire.latitude.toString()+", "+fire.longitude.toString(),style=MaterialTheme.typography.titleSmall)
     Text(fire.acquisition_date+" · "+(fire.acquisition_time ?: "Time unavailable")+" UTC")
     Text("Satellite: "+(fire.satellite ?: "Unknown")+" · Confidence: "+(fire.confidence ?: "Not reported"))
     Text("Fire radiative power: "+(fire.frp?.toString()?.plus(" MW") ?: "Not reported"))
    }
   }
  }
  Text("A detection is a satellite thermal anomaly, not a confirmed fire perimeter or emergency alert.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
