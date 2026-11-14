package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.data.remote.ApiClient
import com.firepulse.app.data.remote.FireObservation
import com.firepulse.app.ui.components.FirePulseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable fun ExploreScreen() {
 var source by remember { mutableStateOf("VIIRS_SNPP_NRT") }
 var days by remember { mutableIntStateOf(1) }
 var observations by remember { mutableStateOf<List<FireObservation>>(emptyList()) }
 var loading by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var expanded by remember { mutableStateOf(false) }
 LaunchedEffect(source,days) {
  loading=true; error=null
  try {
   val response=withContext(Dispatchers.IO) { ApiClient.firePulseApi.getFires(68.0,6.0,98.0,37.5,days,source) }
   observations=response.observations
  } catch(e:Exception) { error=e.localizedMessage ?: "Unable to reach FirePulse API" }
  finally { loading=false }
 }
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Explore",style=MaterialTheme.typography.headlineLarge)
  Text("India · WGS84 bounding box · NASA FIRMS",color=MaterialTheme.colorScheme.onSurfaceVariant)
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
  if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let { Text("Could not load observations: "+it,color=MaterialTheme.colorScheme.error) }
  Text(observations.size.toString()+" detections",style=MaterialTheme.typography.titleMedium)
  if(observations.isEmpty() && !loading && error==null) Text("No detections returned for this area and time window.")
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
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
