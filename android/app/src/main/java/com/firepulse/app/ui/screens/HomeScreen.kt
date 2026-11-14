package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.data.remote.ApiClient
import com.firepulse.app.ui.components.FirePulseCard
import com.firepulse.app.ui.theme.Ember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable fun HomeScreen(onExplore:()->Unit) {
 var count by remember { mutableIntStateOf(0) }
 var loading by remember { mutableStateOf(true) }
 var status by remember { mutableStateOf("Connecting to FirePulse API…") }
 LaunchedEffect(Unit) {
  try {
   val result = withContext(Dispatchers.IO) { ApiClient.firePulseApi.getFires(68.0,6.0,98.0,37.5,1,"VIIRS_SNPP_NRT") }
   count = result.count
   status = "NASA FIRMS data received · " + result.source
  } catch (e: Exception) {
   status = "Could not load live observations. " + (e.localizedMessage ?: "Check API configuration.")
  } finally { loading = false }
 }
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Text("FIREPULSE",style=MaterialTheme.typography.labelLarge,color=Ember)
  Text("Fire intelligence,\nfrom space.",style=MaterialTheme.typography.headlineLarge)
  Text("Satellite-observed active fire activity, organized for clearer understanding.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  FirePulseCard(Modifier.fillMaxWidth()) {
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.Public,null,tint=Ember);Text("Global observation explorer",style=MaterialTheme.typography.titleMedium)}
   Spacer(Modifier.height(8.dp));Text("Explore MODIS and VIIRS detections from NASA FIRMS.")
   TextButton(onClick=onExplore){Text("Explore observations")}
  }
  FirePulseCard(Modifier.fillMaxWidth()) {
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.LocalFireDepartment,null,tint=Ember);Text("Latest observation snapshot",style=MaterialTheme.typography.titleMedium)}
   Text(if(loading) "Loading satellite observations…" else count.toString() + " detections in the latest VIIRS SNPP request",style=MaterialTheme.typography.headlineSmall)
   Text(status,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
  }
  Spacer(Modifier.weight(1f))
  Text("NASA FIRMS data · Research and situational awareness only",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
