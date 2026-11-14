package com.firepulse.app.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable fun SettingsScreen() {
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Settings",style=MaterialTheme.typography.headlineLarge)
  Text("FirePulse configuration",style=MaterialTheme.typography.titleMedium)
  Text("API endpoint, refresh preferences, map settings and attribution controls will be configured here.")
  HorizontalDivider()
  Text("Data source: NASA Fire Information for Resource Management System (FIRMS)")
  Text("Research and situational awareness only. Not an emergency alert service.")
 }
}
