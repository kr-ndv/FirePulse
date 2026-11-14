package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.ui.components.FirePulseCard
import com.firepulse.app.ui.theme.Ember
@Composable fun HomeScreen(onExplore:()->Unit) {
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
   Icon(Icons.Default.LocalFireDepartment,null,tint=Ember);Text("Observation status",style=MaterialTheme.typography.titleMedium)
   Text("Live data status will appear when the FirePulse API is configured.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Spacer(Modifier.weight(1f))
  Text("NASA FIRMS data · Research and situational awareness",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
