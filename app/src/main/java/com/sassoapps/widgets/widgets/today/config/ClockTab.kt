package com.sassoapps.widgets.widgets.today.config

import androidx.compose.runtime.Composable
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.ui.ChoiceRow
import com.sassoapps.widgets.core.ui.LaunchTargetRow
import com.sassoapps.widgets.core.ui.SettingsGroup
import com.sassoapps.widgets.core.ui.SliderRow
import com.sassoapps.widgets.core.ui.SwitchRow
import com.sassoapps.widgets.widgets.today.ClockConfig
import com.sassoapps.widgets.widgets.today.DateFormat
import com.sassoapps.widgets.widgets.today.HourFormat
import com.sassoapps.widgets.widgets.today.TodayConfig

@Composable
internal fun ClockTab(config: TodayConfig, update: ((TodayConfig) -> TodayConfig) -> Unit) {
    val clock = config.clock
    fun edit(transform: (ClockConfig) -> ClockConfig) = update { it.copy(clock = transform(it.clock)) }

    SettingsGroup("Time") {
        ChoiceRow("Format", HourFormat.entries, clock.hourFormat, { it.label }) { f -> edit { it.copy(hourFormat = f) } }
        SwitchRow("Leading zero", clock.leadingZero, { v -> edit { it.copy(leadingZero = v) } }, subtitle = "09:30 instead of 9:30")
        if (clock.hourFormat != HourFormat.H24) {
            SwitchRow("Show AM/PM", clock.showAmPm, { v -> edit { it.copy(showAmPm = v) } })
        }
        SliderRow("Size", clock.timeSizeSp, 24..96, { v -> edit { it.copy(timeSizeSp = v) } })
        ChoiceRow("Font", FontChoice.entries, clock.font, { it.label }) { f -> edit { it.copy(font = f) } }
    }
    SettingsGroup("Date") {
        SwitchRow("Show date", clock.showDate, { v -> edit { it.copy(showDate = v) } })
        if (clock.showDate) {
            ChoiceRow("Format", DateFormat.entries, clock.dateFormat, { it.label }) { f -> edit { it.copy(dateFormat = f) } }
            SliderRow("Size", clock.dateSizeSp, 10..32, { v -> edit { it.copy(dateSizeSp = v) } })
        }
    }
    SettingsGroup("Tap action") {
        LaunchTargetRow("Tapping the clock opens", clock.tapTarget, "Alarms (default clock app)") { t -> edit { it.copy(tapTarget = t) } }
    }
}
