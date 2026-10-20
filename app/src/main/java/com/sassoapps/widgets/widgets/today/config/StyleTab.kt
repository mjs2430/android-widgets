package com.sassoapps.widgets.widgets.today.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.ThemePreset
import com.sassoapps.widgets.core.style.WidgetStyle
import com.sassoapps.widgets.core.ui.ChoiceRow
import com.sassoapps.widgets.core.ui.ColorRow
import com.sassoapps.widgets.core.ui.SettingsGroup
import com.sassoapps.widgets.core.ui.SliderRow
import com.sassoapps.widgets.widgets.today.TodayConfig
import kotlin.math.roundToInt

@Composable
internal fun StyleTab(config: TodayConfig, update: ((TodayConfig) -> TodayConfig) -> Unit) {
    val style = config.style
    fun edit(transform: (WidgetStyle) -> WidgetStyle) = update { it.copy(style = transform(it.style)) }
    val theme = style.theme

    SettingsGroup("Theme") {
        ChoiceRow("Background", ThemePreset.entries, theme, { it.label }) { t -> edit { it.copy(theme = t) } }
        if (theme == ThemePreset.LIQUID_GLASS || theme == ThemePreset.SOFT_GLASS) {
            Text(
                "Widgets can't blur the wallpaper, so glass is a tinted, highlighted pane.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (theme != ThemePreset.TRANSPARENT) {
            SliderRow("Opacity", (style.opacity * 100).roundToInt(), 0..100, { v -> edit { it.copy(opacity = v / 100f) } }) { "$it%" }
        }
        when (theme) {
            ThemePreset.LIQUID_GLASS, ThemePreset.SOFT_GLASS ->
                ColorRow("Tint", style.backgroundColor, { c -> edit { it.copy(backgroundColor = c) } })
            ThemePreset.SOLID ->
                ColorRow("Color", style.backgroundColor, { c -> edit { it.copy(backgroundColor = c) } })
            ThemePreset.GRADIENT -> {
                ColorRow("Start color", style.backgroundColor, { c -> edit { it.copy(backgroundColor = c) } })
                ColorRow("End color", style.gradientEndColor, { c -> edit { it.copy(gradientEndColor = c) } })
            }
            ThemePreset.TRANSPARENT, ThemePreset.MATERIAL_YOU -> Unit
        }
    }
    SettingsGroup("Shape") {
        SliderRow("Corner radius", style.cornerRadiusDp, 0..40, { v -> edit { it.copy(cornerRadiusDp = v) } }) { "$it dp" }
        SliderRow("Padding", style.paddingDp, 4..32, { v -> edit { it.copy(paddingDp = v) } }) { "$it dp" }
    }
    SettingsGroup("Text") {
        if (theme == ThemePreset.MATERIAL_YOU) {
            Text("Material You picks text colors from your wallpaper.", style = MaterialTheme.typography.bodySmall)
        } else {
            ColorRow("Text color", style.textColor, { c -> edit { it.copy(textColor = c) } })
        }
        ChoiceRow("Font (event and weather)", FontChoice.entries, style.fontFamily, { it.label }) { f -> edit { it.copy(fontFamily = f) } }
    }
}
