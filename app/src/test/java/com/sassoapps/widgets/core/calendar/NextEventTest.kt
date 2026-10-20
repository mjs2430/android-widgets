package com.sassoapps.widgets.core.calendar

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextEventTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private fun at(hour: Int, minute: Int = 0, day: Int = 20) =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private val now = at(10)

    private fun event(title: String, begin: Long, end: Long, allDay: Boolean = false, declined: Boolean = false) =
        CalendarEvent(0, 0, title, begin, end, allDay, null, 0, declined)

    @Test
    fun `picks the earliest timed event that has not ended`() {
        val events = listOf(
            event("Over", at(8), at(9)),
            event("Later", at(15), at(16)),
            event("Soon", at(11), at(12)),
        )
        assertEquals("Soon", NextEvent.upcoming(events, now, includeAllDay = false).firstOrNull()?.title)
    }

    @Test
    fun `ongoing events count until they end`() {
        val events = listOf(event("Ongoing", at(9, 30), at(10, 30)), event("Next", at(11), at(12)))
        assertEquals("Ongoing", NextEvent.upcoming(events, now, includeAllDay = false).firstOrNull()?.title)
    }

    @Test
    fun `skips declined events`() {
        val events = listOf(event("Declined", at(11), at(12), declined = true), event("Accepted", at(13), at(14)))
        assertEquals("Accepted", NextEvent.upcoming(events, now, includeAllDay = false).firstOrNull()?.title)
    }

    @Test
    fun `all-day events only show when enabled and nothing timed is coming`() {
        val allDay = event("Holiday", at(0), at(0, day = 21), allDay = true)
        assertTrue(NextEvent.upcoming(listOf(allDay), now, includeAllDay = false).isEmpty())
        assertEquals("Holiday", NextEvent.upcoming(listOf(allDay), now, includeAllDay = true).firstOrNull()?.title)
        val timed = event("Meeting", at(14), at(15))
        assertEquals("Meeting", NextEvent.upcoming(listOf(allDay, timed), now, includeAllDay = true).firstOrNull()?.title)
    }

    @Test
    fun `next change is the event start, its end, or midnight`() {
        assertEquals(at(11), NextEvent.nextChangeAt(event("A", at(11), at(12)), now, zone))
        assertEquals(at(10, 30), NextEvent.nextChangeAt(event("B", at(9), at(10, 30)), now, zone))
        assertEquals(at(0, day = 21), NextEvent.nextChangeAt(null, now, zone))
    }

    @Test
    fun `upcoming lists every event that has not ended, soonest first`() {
        val events = listOf(event("Later", at(15), at(16)), event("Over", at(8), at(9)), event("Soon", at(11), at(12)))
        assertEquals(listOf("Soon", "Later"), NextEvent.upcoming(events, now, includeAllDay = false).map { it.title })
    }

    @Test
    fun `day labels say now, today, tomorrow, or the weekday`() {
        fun label(e: CalendarEvent) = EventTimeFormatter.dayLabel(e, now, zone, Locale.US)
        assertEquals("Now", label(event("A", at(9), at(11))))
        assertEquals("Today", label(event("A", at(15), at(16))))
        assertEquals("Tomorrow", label(event("A", at(9, day = 21), at(10, day = 21))))
        assertEquals("Thursday", label(event("A", at(9, day = 22), at(10, day = 22))))
        assertEquals("Today", label(event("A", at(0), at(0, day = 21), allDay = true)))
    }

    @Test
    fun `time ranges follow the clock format`() {
        fun range(e: CalendarEvent, use24h: Boolean) = EventTimeFormatter.timeRange(e, zone, use24h, Locale.US)
        assertEquals("9:00 AM – 10:30 AM", range(event("A", at(9), at(10, 30)), use24h = false))
        assertEquals("15:00 – 16:00", range(event("A", at(15), at(16)), use24h = true))
        assertEquals("All day", range(event("A", at(0), at(0, day = 21), allDay = true), use24h = false))
        assertEquals("Oct 22", EventTimeFormatter.date(event("A", at(9, day = 22), at(10, day = 22)), zone, Locale.US))
    }

    @Test
    fun `an event with no end time shows only its start`() {
        val noEnd = CalendarEvent(0, 0, "Dinner", at(18), at(19), false, null, 0, false, endKnown = false)
        assertEquals("6:00 PM", EventTimeFormatter.timeRange(noEnd, zone, use24h = false, Locale.US))
        assertEquals("6:00 PM – 7:00 PM", EventTimeFormatter.timeRange(noEnd.copy(endKnown = true), zone, use24h = false, Locale.US))
    }
}
