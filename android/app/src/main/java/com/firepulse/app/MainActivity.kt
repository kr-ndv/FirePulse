package com.firepulse.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.firepulse.app.ui.FirePulseApp
import com.firepulse.app.ui.theme.FirePulseTheme
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); enableEdgeToEdge()
  setContent { FirePulseTheme { FirePulseApp() } }
 }
}
