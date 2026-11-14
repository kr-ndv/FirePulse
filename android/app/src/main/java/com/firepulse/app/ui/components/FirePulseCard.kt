package com.firepulse.app.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firepulse.app.ui.theme.PanelShape
@Composable fun FirePulseCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Card(modifier=modifier,shape=PanelShape,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(18.dp),content=content) }
}
