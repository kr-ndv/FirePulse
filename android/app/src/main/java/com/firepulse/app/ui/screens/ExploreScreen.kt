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
import com.firepulse.app.data.remote.FireCluster
import com.firepulse.app.ui.components.FireActivityMap
import com.firepulse.app.ui.components.FirePulseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ExploreArea(val name: String, val west: Double, val south: Double, val east: Double, val north: Double)

private val exploreAreas = listOf(
 ExploreArea("All India",68.0,6.0,98.0,37.5),
 ExploreArea("North India",73.5,26.0,89.0,37.5),
 ExploreArea("Central India",74.0,20.0,85.0,27.5),
 ExploreArea("East India",83.0,18.0,97.0,28.5),
 ExploreArea("West India",68.0,15.0,78.5,27.5),
 ExploreArea("South India",74.0,7.0,85.5,20.5),
 ExploreArea("Northeast India",89.0,21.0,98.0,30.5)
)

@Composable fun ExploreScreen() {
 val context=LocalContext.current
 val dao=remember { FirePulseDatabase.getInstance(context).fireObservationDao() }
 var source by remember { mutableStateOf("VIIRS_NOAA21_NRT") }
 var selectedArea by remember { mutableStateOf(exploreAreas.first()) }
 var areaMenuExpanded by remember { mutableStateOf(false) }
 var days by remember { mutableIntStateOf(1) }
 var observations by remember { mutableStateOf<List<FireObservation>>(emptyList()) }
 var clusters by remember { mutableStateOf<List<FireCluster>>(emptyList()) }
 var clusterError by remember { mutableStateOf<String?>(null) }
 var loading by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var expanded by remember { mutableStateOf(false) }
 var showingCache by remember { mutableStateOf(false) }
 var apiDataStatus by remember { mutableStateOf("live") }
 var cachedAt by remember { mutableStateOf<String?>(null) }

 LaunchedEffect(source,days,selectedArea) {
  loading=true; error=null; showingCache=false
  clusters=emptyList(); clusterError=null
  val cached=if(selectedArea.name=="All India") withContext(Dispatchers.IO) {
   dao.getSnapshot(source,days).map { it.toObservation() }
  } else emptyList()
  if(cached.isNotEmpty()) {
   observations=cached
   apiDataStatus="offline_cache"
   cachedAt=null
   showingCache=true
  } else {
   observations=emptyList()
   apiDataStatus="live"
   cachedAt=null
  }
  try {
   val response=withContext(Dispatchers.IO) {
    ApiClient.firePulseApi.getFires(selectedArea.west,selectedArea.south,selectedArea.east,selectedArea.north,days,source)
   }
   observations=response.observations
   apiDataStatus=response.data_status ?: "live"
   cachedAt=response.cached_at
   showingCache=apiDataStatus == "stale_cache"
   try {
    val clusterResponse=withContext(Dispatchers.IO) {
     ApiClient.firePulseApi.getClusters(selectedArea.west,selectedArea.south,selectedArea.east,selectedArea.north,days,source,5.0)
    }
    clusters=clusterResponse.clusters
    clusterError=null
   } catch(clusterException:Exception) {
    clusters=emptyList()
    clusterError=clusterException.localizedMessage ?: "Cluster overlay unavailable"
   }
   if(apiDataStatus=="live" && selectedArea.name=="All India") {
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
  Text("Satellite-observed active fire detections",color=MaterialTheme.colorScheme.onSurfaceVariant)
  ExposedDropdownMenuBox(expanded=areaMenuExpanded,onExpandedChange={areaMenuExpanded=it}) {
   OutlinedTextField(
    value=selectedArea.name,onValueChange={},readOnly=true,
    label={Text("Geographic search area")},
    modifier=Modifier.fillMaxWidth().menuAnchor(),
    trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded=areaMenuExpanded)}
   )
   ExposedDropdownMenu(expanded=areaMenuExpanded,onDismissRequest={areaMenuExpanded=false}) {
    exploreAreas.forEach { area ->
     DropdownMenuItem(text={Text(area.name)},onClick={selectedArea=area;areaMenuExpanded=false})
    }
   }
  }
  Text("Area presets use approximate rectangular bounds, not official state or district borders.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.tertiary)
  ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}) {
   OutlinedTextField(value=source,onValueChange={},readOnly=true,label={Text("Satellite feed")},modifier=Modifier.fillMaxWidth().menuAnchor())
   ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
    listOf("VIIRS_NOAA21_NRT","VIIRS_NOAA20_NRT","MODIS_NRT").forEach { item ->
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
   Text(selectedArea.name+" fire activity",style=MaterialTheme.typography.titleMedium)
   Text(observations.size.toString()+" satellite detections",style=MaterialTheme.typography.headlineMedium)
   Text("Feed: "+source+" · Window: "+days+" day(s)",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text("Search bounds: %.1f°–%.1f° E, %.1f°–%.1f° N".format(selectedArea.west,selectedArea.east,selectedArea.south,selectedArea.north),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(apiDataStatus=="offline_cache") Text("Offline device cache",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
   else if(showingCache) Text("Recent server snapshot · NASA feed unavailable",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
   else if(!loading && error==null) Text("Live response",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
   cachedAt?.let { Text("Snapshot captured: "+it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let { Text(it,color=if(showingCache) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error) }
  FireActivityMap(observations=observations,clusters=clusters)
  Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
   Text("● Satellite detections",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text("● Activity clusters",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.tertiary)
  }
  clusterError?.let { Text("Cluster overlay unavailable: $it",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  if(clusters.isNotEmpty()) Text("${clusters.size} potential activity clusters · 5 km grouping radius",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(selectedArea.name!="All India") Text("Regional results are fetched live; offline snapshots are currently available for All India only.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
