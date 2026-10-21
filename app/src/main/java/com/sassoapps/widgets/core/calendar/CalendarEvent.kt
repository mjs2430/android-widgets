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
    /** False for an event saved without an end time; [end] is then a guess (see CalendarRepository). */
    val endKnown: Boolean = true,
)

object NextEvent {

    /**
     * Events that haven't ended, soonest first, for paging through on the widget.
     * Timed events come first; all-day events follow only when [includeAllDay] is set.
     */
    fun upcoming(events: List<CalendarEvent>, now: Long, includeAllDay: Boolean): List<CalendarEvent> {
        val candidates = events.filter { !it.declined && it.end > now }.sortedBy { it.begin }
        return candidates.filter { !it.allDay } + if (includeAllDay) candidates.filter { it.allDay } else emptyList()
    }

    /**
     * When the widget's events next change: the first event starts, it ends,
     * or the day rolls over (which turns "Tomorrow" into "Today").
     */
    fun nextChangeAt(event: CalendarEvent?, now: Long, zone: ZoneId): Long {
        val midnight = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        return listOfNotNull(event?.begin, event?.end, midnight).filter { it > now }.min()
    }
}

object EventTimeFormatter {

    /** "Now", "Today", "Tomorrow", or the weekday, e.g. "Wednesday". */
    fun dayLabel(event: CalendarEvent, now: Long, zone: ZoneId, locale: Locale = Locale.getDefault()): String {
        if (event.begin <= now && !event.allDay) return "Now"
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val day = Instant.ofEpochMilli(event.begin).atZone(zone).toLocalDate()
        return when {
            !day.isAfter(today) -> "Today"
            day == today.plusDays(1) -> "Tomorrow"
            else -> day.format(DateTimeFormatter.ofPattern("EEEE", locale))
        }
    }

    /** "9:00 AM – 10:00 AM", just the start for an event with no end time, or "All day". */
    fun timeRange(event: CalendarEvent, zone: ZoneId, use24h: Boolean, locale: Locale = Locale.getDefault()): String {
        if (event.allDay) return "All day"
        val time = DateTimeFormatter.ofPattern(if (use24h) "H:mm" else "h:mm a", locale)
        fun at(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).format(time)
        return if (event.endKnown) "${at(event.begin)} – ${at(event.end)}" else at(event.begin)
    }

    /** The event's date, e.g. "Oct 20". */
    fun date(event: CalendarEvent, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
        Instant.ofEpochMilli(event.begin).atZone(zone).format(DateTimeFormatter.ofPattern("MMM d", locale))
}
