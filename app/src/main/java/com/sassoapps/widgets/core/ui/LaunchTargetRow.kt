package com.sassoapps.widgets.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sassoapps.widgets.appContainer
import com.sassoapps.widgets.core.apps.InstalledApp
import com.sassoapps.widgets.core.apps.LaunchTarget

/** Shows what a tap opens, and lets the user change it to the default, an app, or nothing. */
@Composable
fun LaunchTargetRow(title: String, target: LaunchTarget, defaultLabel: String, onChange: (LaunchTarget) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val current = when (target) {
        LaunchTarget.Default -> defaultLabel
        is LaunchTarget.App -> target.label
        LaunchTarget.None -> "Nothing"
    }
    Column(Modifier.fillMaxWidth().clickable { picking = true }.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(current, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    if (picking) {
        AppPickerDialog(
            defaultLabel = defaultLabel,
            onDismiss = { picking = false },
            onPick = {
                onChange(it)
                picking = false
            },
        )
    }
}

@Composable
private fun AppPickerDialog(defaultLabel: String, onDismiss: () -> Unit, onPick: (LaunchTarget) -> Unit) {
    val context = LocalContext.current
    val apps by produceState<List<InstalledApp>?>(null) { value = context.appContainer.apps.launchableApps() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("When tapped, open…") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item { PickerRow(defaultLabel) { onPick(LaunchTarget.Default) } }
                item { PickerRow("Nothing") { onPick(LaunchTarget.None) } }
                item { HorizontalDivider() }
                if (apps == null) item { Text("Loading apps…", Modifier.padding(12.dp)) }
                items(apps.orEmpty(), key = { it.packageName }) { app ->
                    PickerRow(app.label, app) { onPick(LaunchTarget.App(app.packageName, app.label)) }
                }
            }
        },
    )
}

@Composable
private fun PickerRow(label: String, app: InstalledApp? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null) {
            val icon = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
            Image(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
