package com.sassoapps.widgets.widgets.today.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.sassoapps.widgets.core.calendar.CalendarEvent
import com.sassoapps.widgets.core.calendar.EventTimeFormatter
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.widgets.today.EventConfig
import java.time.ZoneId

/** The next calendar event, or a prompt to grant calendar access. Shows nothing when the calendar is clear. */
@Composable
fun EventSection(
    event: CalendarEvent?,
    hasPermission: Boolean,
    now: Long,
    use24h: Boolean,
    config: EventConfig,
    palette: WidgetPalette,
    font: FontChoice,
    compact: Boolean,
) {
    if (!hasPermission) {
        WidgetText("Tap to show your next event", palette.secondaryText, config.textSizeSp, font)
        return
    }
    if (event == null) return

    val time = EventTimeFormatter.format(event, now, ZoneId.systemDefault(), use24h)
    val details = listOfNotNull(time, event.location.takeIf { config.showLocation }).joinToString(" · ")

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (config.showCalendarColor) {
            Box(GlanceModifier.size(DOT_SIZE.dp).cornerRadius((DOT_SIZE / 2).dp).background(Color(event.color))) {}
            Spacer(GlanceModifier.width(8.dp))
        }
        if (compact) {
            WidgetText("${event.title} · $time", palette.text, config.textSizeSp, font)
        } else {
            Column {
                WidgetText(event.title, palette.text, config.textSizeSp, font, bold = true)
                WidgetText(details, palette.secondaryText, config.textSizeSp * SECONDARY_SCALE, font)
            }
        }
    }
}

private const val DOT_SIZE = 8
private const val SECONDARY_SCALE = 0.87f
