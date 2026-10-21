package com.sassoapps.widgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import com.sassoapps.widgets.core.ui.GlancePreview
import com.sassoapps.widgets.core.ui.WidgetsTheme
import com.sassoapps.widgets.core.ui.rememberPermissionState
import kotlinx.coroutines.launch

/** Home screen of the app: the widget catalog, placed widgets, and permissions. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WidgetsTheme { HomeScreen() } }
    }

    override fun onResume() {
        super.onResume()
        // Foreground-only location: refresh it whenever the app is opened.
        lifecycleScope.launch {
            val container = appContainer
            container.location.refreshDevicePlace()?.let { container.weather.refresh(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Sasso Widgets") }) }) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            items(WidgetCatalog) { WidgetCard(it) }
            item { PermissionsCard() }
            item { AboutCard() }
        }
    }
}

@Composable
private fun WidgetCard(widget: WidgetDefinition) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val provider = remember { ComponentName(context, widget.receiver) }
    var placedIds by remember { mutableStateOf(manager.getAppWidgetIds(provider).toList()) }
    LifecycleResumeEffect(Unit) {
        placedIds = manager.getAppWidgetIds(provider).toList()
        onPauseOrDispose {}
    }

    Card {
        Column(Modifier.padding(16.dp)) {
            Text(widget.title, style = MaterialTheme.typography.titleLarge)
            Text(widget.description, style = MaterialTheme.typography.bodyMedium)
            BoxWithConstraints(
                Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3A6073), Color(0xFF16222A), Color(0xFF8E54E9))))
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlancePreview(key = widget.title, size = DpSize(maxWidth, 200.dp), content = widget.preview)
            }
            if (manager.isRequestPinAppWidgetSupported) {
                Button(onClick = { manager.requestPinAppWidget(provider, null, null) }) { Text("Add to home screen") }
            } else {
                Text("Long-press your home screen, choose Widgets, and find “${widget.title}”.")
            }
            placedIds.forEachIndexed { index, id ->
                Text(
                    "Customize ${widget.title} widget" + if (placedIds.size > 1) " #${index + 1}" else "",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(context, widget.configActivity).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                            )
                        }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard() {
    val calendar = rememberPermissionState(Manifest.permission.READ_CALENDAR)
    val location = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            PermissionRow("Calendar", "Shows your next event. Stays on this device.", calendar.granted, calendar.request)
            PermissionRow("Approximate location", "Local weather. Or pick a city per widget.", location.granted, location.request)
        }
    }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, request: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) Text("Allowed", color = MaterialTheme.colorScheme.primary) else OutlinedButton(onClick = request) { Text("Allow") }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    Card(Modifier.padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(context.appContainer.weather.attribution, style = MaterialTheme.typography.bodyMedium)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
