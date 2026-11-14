package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.ui.components.FirePulseCard
@Composable fun InsightsScreen() {
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Insights",style=MaterialTheme.typography.headlineLarge)
  FirePulseCard(Modifier.fillMaxWidth()){Text("Historical activity",style=MaterialTheme.typography.titleMedium);Text("Daily and seasonal trends from compatible historical FIRMS products.")}
  FirePulseCard(Modifier.fillMaxWidth()){Text("Sensor comparison",style=MaterialTheme.typography.titleMedium);Text("Compare MODIS and VIIRS without assuming raw detection counts are interchangeable.")}
  FirePulseCard(Modifier.fillMaxWidth()){Text("Anomaly analysis",style=MaterialTheme.typography.titleMedium);Text("Baseline-based unusual-activity indicators will follow once historical data is available.")}
 }
}
