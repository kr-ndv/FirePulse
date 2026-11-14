package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.ui.components.FirePulseCard
@Composable fun ExploreScreen() {
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Explore",style=MaterialTheme.typography.headlineLarge)
  Text("Global active fire observations",color=MaterialTheme.colorScheme.onSurfaceVariant)
  FirePulseCard(Modifier.fillMaxWidth().weight(1f)) {
   Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Icon(Icons.Default.Map,null,Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary)
     Text("Interactive map integration",style=MaterialTheme.typography.titleMedium)
     Text("MapLibre and NASA FIRMS hotspot layers are the next milestone.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
 }
}
