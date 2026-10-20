package com.sassoapps.widgets.widgets.today.config

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sassoapps.widgets.core.calendar.CalendarInfo
import com.sassoapps.widgets.core.ui.ChoiceRow
import com.sassoapps.widgets.core.ui.LaunchTargetRow
import com.sassoapps.widgets.core.ui.SettingsGroup
import com.sassoapps.widgets.core.ui.SliderRow
import com.sassoapps.widgets.core.ui.SwitchRow
import com.sassoapps.widgets.core.ui.rememberPermissionState
import com.sassoapps.widgets.widgets.today.EventConfig
import com.sassoapps.widgets.widgets.today.TodayConfig

private val LookaheadOptions = listOf(6, 12, 24, 48, 72)

@Composable
internal fun CalendarTab(config: TodayConfig, viewModel: TodayConfigViewModel, update: ((TodayConfig) -> TodayConfig) -> Unit) {
    val event = config.event
    fun edit(transform: (EventConfig) -> EventConfig) = update { it.copy(event = transform(it.event)) }
    val permission = rememberPermissionState(Manifest.permission.READ_CALENDAR) { granted -> if (granted) viewModel.loadCalendars() }
    val calendars by viewModel.calendars.collectAsState()

    if (!permission.granted) {
        SettingsGroup("Calendar access") {
            Text("Allow calendar access to show your next event. Events never leave your device.")
            Button(onClick = permission.request, modifier = Modifier.padding(vertical = 8.dp)) { Text("Allow access") }
        }
        return
    }

    SettingsGroup("Calendars") {
        Text(
            if (event.calendarIds.isEmpty()) "Showing events from all calendars" else "Showing ${event.calendarIds.size} selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        calendars.groupBy { it.accountName }.forEach { (account, inAccount) ->
            Text(account, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            inAccount.forEach { calendar ->
                // An empty selection means "all", so every box starts checked.
                val checked = event.calendarIds.isEmpty() || calendar.id in event.calendarIds
                CalendarRow(calendar, checked) { nowChecked ->
                    edit { it.copy(calendarIds = toggle(it.calendarIds, calendars, calendar.id, nowChecked)) }
                }
            }
        }
    }
    SettingsGroup("Next event") {
        ChoiceRow("Look ahead", LookaheadOptions, event.lookaheadHours, { "${it}h" }) { h -> edit { it.copy(lookaheadHours = h) } }
        SwitchRow("Include all-day events", event.includeAllDay, { v -> edit { it.copy(includeAllDay = v) } }, subtitle = "Shown only when nothing timed is coming up")
        SwitchRow("Show location", event.showLocation, { v -> edit { it.copy(showLocation = v) } })
        SwitchRow("Show calendar color", event.showCalendarColor, { v -> edit { it.copy(showCalendarColor = v) } })
        SliderRow("Text size", event.textSizeSp, 10..28, { v -> edit { it.copy(textSizeSp = v) } })
    }
    SettingsGroup("Tap action") {
        SwitchRow("Open the event itself", event.openEventOnTap, { v -> edit { it.copy(openEventOnTap = v) } })
        if (!event.openEventOnTap) {
            LaunchTargetRow("Tapping the event opens", event.tapTarget, "Default calendar app") { t -> edit { it.copy(tapTarget = t) } }
        }
    }
}

/** Updates the selection, collapsing "everything checked" back to the empty "all" set. */
private fun toggle(selected: Set<Long>, all: List<CalendarInfo>, id: Long, checked: Boolean): Set<Long> {
    val current = selected.ifEmpty { all.map { it.id }.toSet() }
    val next = if (checked) current + id else current - id
    return if (next.size == all.size) emptySet() else next
}

@Composable
private fun CalendarRow(calendar: CalendarInfo, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Box(Modifier.size(12.dp).background(Color(calendar.color), CircleShape))
        Spacer(Modifier.width(10.dp))
        Column { Text(calendar.name, style = MaterialTheme.typography.bodyLarge) }
    }
}
