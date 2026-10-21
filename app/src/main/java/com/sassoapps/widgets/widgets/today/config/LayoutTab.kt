package com.sassoapps.widgets.widgets.today.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sassoapps.widgets.core.ui.ChoiceRow
import com.sassoapps.widgets.core.ui.SettingsGroup
import com.sassoapps.widgets.core.ui.SwitchRow
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.TodayLayout
import com.sassoapps.widgets.widgets.today.TodaySection

@Composable
internal fun LayoutTab(config: TodayConfig, update: ((TodayConfig) -> TodayConfig) -> Unit) {
    SettingsGroup("Layout") {
        ChoiceRow("Arrangement", TodayLayout.entries, config.layout, { it.label }) { layout -> update { it.copy(layout = layout) } }
        Text(config.layout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SettingsGroup("Show") {
        TodaySection.entries.forEach { section ->
            SwitchRow(section.label, config.isVisible(section), { visible ->
                update { it.copy(hidden = if (visible) it.hidden - section else it.hidden + section) }
            })
        }
    }
}
