package com.sassoapps.widgets.widgets.today.config

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sassoapps.widgets.core.ui.ChoiceRow
import com.sassoapps.widgets.core.ui.SettingsGroup
import com.sassoapps.widgets.widgets.today.SectionSlot
import com.sassoapps.widgets.widgets.today.TodayAlignment
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.TodayLayout

@Composable
internal fun LayoutTab(config: TodayConfig, update: ((TodayConfig) -> TodayConfig) -> Unit) {
    SettingsGroup("Arrangement") {
        ChoiceRow("Layout", TodayLayout.entries, config.layout, { it.label }) { layout -> update { it.copy(layout = layout) } }
        Text(config.layout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceRow("Alignment", TodayAlignment.entries, config.alignment, { it.label }) { a -> update { it.copy(alignment = a) } }
    }
    SettingsGroup("Sections") {
        config.sections.forEachIndexed { index, slot ->
            SectionRow(
                slot = slot,
                canMoveUp = index > 0,
                canMoveDown = index < config.sections.lastIndex,
                onToggle = { visible -> update { it.copy(sections = it.sections.replace(index, slot.copy(visible = visible))) } },
                onMove = { delta -> update { it.copy(sections = it.sections.move(index, index + delta)) } },
            )
        }
    }
}

@Composable
private fun SectionRow(
    slot: SectionSlot,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = slot.visible, onCheckedChange = onToggle)
        Text(slot.section.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Text("↑") }
        TextButton(onClick = { onMove(1) }, enabled = canMoveDown) { Text("↓") }
    }
}

private fun <T> List<T>.replace(index: Int, item: T) = toMutableList().also { it[index] = item }

private fun <T> List<T>.move(from: Int, to: Int) = toMutableList().also { it.add(to, it.removeAt(from)) }
