package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.ui.components.FirePulseCard
@Composable fun CalendarScreen() {
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Activity calendar",style=MaterialTheme.typography.headlineLarge)
  Text("Daily satellite-observed activity by region",color=MaterialTheme.colorScheme.onSurfaceVariant)
  FirePulseCard(Modifier.fillMaxWidth()) {
   Text("Harmonized calendar",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(8.dp))
   Text("Daily MODIS and VIIRS observations will be aggregated to a common spatial grid while retaining separate sensor counts.")
  }
 }
}
