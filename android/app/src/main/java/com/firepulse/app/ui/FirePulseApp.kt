package com.firepulse.app.ui
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.firepulse.app.ui.screens.*
private enum class Tab(val title:String) { Home("Home"), Explore("Explore"), Calendar("Calendar"), Insights("Insights"), Settings("Settings") }
@Composable fun FirePulseApp() {
 var tab by remember { mutableStateOf(Tab.Home) }
 Scaffold(bottomBar={ NavigationBar { Tab.entries.forEach { item ->
  val icon=when(item){Tab.Home->Icons.Default.Home;Tab.Explore->Icons.Default.Explore;Tab.Calendar->Icons.Default.CalendarMonth;Tab.Insights->Icons.Default.Insights;Tab.Settings->Icons.Default.Settings}
  NavigationBarItem(selected=tab==item,onClick={tab=item},icon={Icon(icon,item.title)},label={Text(item.title)})
 } } }) { padding ->
  androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
   when(tab){Tab.Home->HomeScreen{tab=Tab.Explore};Tab.Explore->ExploreScreen();Tab.Calendar->CalendarScreen();Tab.Insights->InsightsScreen();Tab.Settings->SettingsScreen()}
  }
 }
}
