package com.sassoapps.widgets.widgets.today.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.sassoapps.widgets.R
import com.sassoapps.widgets.core.calendar.CalendarEvent
import com.sassoapps.widgets.core.calendar.EventTimeFormatter
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.widgets.today.EventConfig
import com.sassoapps.widgets.widgets.today.EventLines
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.DETAIL_SCALE
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.EVENT_GAP
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.LABEL_SCALE
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.TITLE_SCALE
import java.time.ZoneId

/** Everything the event views need besides the event itself. */
data class EventLook(
    val config: EventConfig,
    val palette: WidgetPalette,
    val font: FontChoice,
    val use24h: Boolean,
    val lines: EventLines,
)

/**
 * The upcoming events as a scrollable list, one event tall: swipe up and down to see the next ones.
 * Each has a bar in its calendar's color on the left, and tapping it opens that event.
 */
@Composable
fun EventList(
    events: List<CalendarEvent>,
    now: Long,
    look: EventLook,
    /** Height of one event, which is also the height of the list: it shows one at a time. */
    itemHeight: Float,
    /** Modifier that opens an event. */
    openEvent: (CalendarEvent) -> GlanceModifier,
) {
    LazyColumn(GlanceModifier.fillMaxWidth().height(itemHeight.dp)) {
        items(events, itemId = { it.eventId xor it.begin }) { event ->
            // Each slot is the event plus room below it, so two events passing the window don't touch.
            Row(
                openEvent(event).fillMaxWidth().height((itemHeight + EVENT_GAP).dp).padding(bottom = EVENT_GAP.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (look.config.showCalendarColor) {
                    Box(GlanceModifier.width(BAR_WIDTH.dp).height((itemHeight - BAR_INSET).dp).cornerRadius((BAR_WIDTH / 2f).dp).background(Color(event.color))) {}
                    Spacer(GlanceModifier.width(BAR_GAP.dp))
                }
                Box(GlanceModifier.defaultWeight()) { EventPage(event, now, look) }
            }
        }
    }
}

/**
 * One calendar event, mocked after At a Glance: a small "TODAY" label with the time beside it,
 * a large title, then the location with a map-pin icon.
 */
@Composable
private fun EventPage(event: CalendarEvent, now: Long, look: EventLook) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val zone = ZoneId.systemDefault()
    val base = look.config.textSizeSp
    val label = EventTimeFormatter.dayLabel(event, now, zone, locale).uppercase(locale)
    val time = EventTimeFormatter.timeRange(event, zone, look.use24h, locale)
    val location = event.location?.takeIf { look.lines.location && look.config.showLocation }

    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetText(label, look.palette.secondaryText, base * LABEL_SCALE, look.font, bold = true)
            WidgetText(
                time,
                look.palette.secondaryText,
                base * DETAIL_SCALE,
                look.font,
                modifier = GlanceModifier.defaultWeight().padding(start = 8.dp),
            )
        }
        WidgetText(event.title, look.palette.text, base * TITLE_SCALE, look.font)
        if (location != null) Detail(R.drawable.ic_location, location, base * DETAIL_SCALE, look)
    }
}

/** Shown in place of events: a permission prompt, or a quiet "nothing coming up". */
@Composable
fun EventPlaceholder(hasPermission: Boolean, look: EventLook) {
    val text = if (hasPermission) "No upcoming events" else "Tap to show your calendar events"
    WidgetText(text, look.palette.secondaryText, look.config.textSizeSp * DETAIL_SCALE, look.font)
}

@Composable
private fun Detail(icon: Int, text: String, sizeSp: Float, look: EventLook) {
    val iconSize = sizeSp * LocalContext.current.resources.configuration.fontScale
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(iconSize.dp),
            colorFilter = ColorFilter.tint(look.palette.secondaryText.provider),
        )
        Spacer(GlanceModifier.width(5.dp))
        WidgetText(text, look.palette.secondaryText, sizeSp, look.font)
    }
}

private const val BAR_WIDTH = 4
private const val BAR_GAP = 10
private const val BAR_INSET = 8
