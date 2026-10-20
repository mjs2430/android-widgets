package com.sassoapps.widgets.core.calendar

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int,
)

/** One occurrence of an event. All-day events use local midnight for [begin] and [end]. */
data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
    val color: Int,
    val declined: Boolean,
)

object NextEvent {

    /** Prefers timed events; falls back to all-day events only when [includeAllDay] is set. */
    fun select(events: List<CalendarEvent>, now: Long, includeAllDay: Boolean): CalendarEvent? {
        val candidates = events.filter { !it.declined && it.end > now }.sortedBy { it.begin }
        return candidates.firstOrNull { !it.allDay }
            ?: candidates.firstOrNull { it.allDay }.takeIf { includeAllDay }
    }

    /**
     * When the widget's event line next changes: the event starts, it ends,
     * or the day rolls over (which turns "Tomorrow" into today).
     */
    fun nextChangeAt(event: CalendarEvent?, now: Long, zone: ZoneId): Long {
        val midnight = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        return listOfNotNull(event?.begin, event?.end, midnight).filter { it > now }.min()
    }
}

object EventTimeFormatter {

    fun format(
        event: CalendarEvent,
        now: Long,
        zone: ZoneId,
        use24h: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String {
        val time = DateTimeFormatter.ofPattern(if (use24h) "H:mm" else "h:mm a", locale)
        val weekday = DateTimeFormatter.ofPattern("EEE", locale)
        val begin = Instant.ofEpochMilli(event.begin).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val day = when (begin.toLocalDate()) {
            today -> null
            today.plusDays(1) -> "Tomorrow"
            else -> begin.format(weekday)
        }

        return when {
            event.allDay -> if (event.begin <= now) "Today" else day ?: "Today"
            event.begin <= now -> "Now · until " + Instant.ofEpochMilli(event.end).atZone(zone).format(time)
            day == null -> begin.format(time)
            else -> "$day ${begin.format(time)}"
        }
    }
}
