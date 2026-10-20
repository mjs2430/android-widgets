package com.sassoapps.widgets.core.calendar

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("Soon", NextEvent.select(events, now, includeAllDay = false)?.title)
    }

    @Test
    fun `ongoing events count until they end`() {
        val events = listOf(event("Ongoing", at(9, 30), at(10, 30)), event("Next", at(11), at(12)))
        assertEquals("Ongoing", NextEvent.select(events, now, includeAllDay = false)?.title)
    }

    @Test
    fun `skips declined events`() {
        val events = listOf(event("Declined", at(11), at(12), declined = true), event("Accepted", at(13), at(14)))
        assertEquals("Accepted", NextEvent.select(events, now, includeAllDay = false)?.title)
    }

    @Test
    fun `all-day events only show when enabled and nothing timed is coming`() {
        val allDay = event("Holiday", at(0), at(0, day = 21), allDay = true)
        assertNull(NextEvent.select(listOf(allDay), now, includeAllDay = false))
        assertEquals("Holiday", NextEvent.select(listOf(allDay), now, includeAllDay = true)?.title)
        val timed = event("Meeting", at(14), at(15))
        assertEquals("Meeting", NextEvent.select(listOf(allDay, timed), now, includeAllDay = true)?.title)
    }

    @Test
    fun `next change is the event start, its end, or midnight`() {
        assertEquals(at(11), NextEvent.nextChangeAt(event("A", at(11), at(12)), now, zone))
        assertEquals(at(10, 30), NextEvent.nextChangeAt(event("B", at(9), at(10, 30)), now, zone))
        assertEquals(at(0, day = 21), NextEvent.nextChangeAt(null, now, zone))
    }

    @Test
    fun `formats event times relative to today`() {
        fun format(e: CalendarEvent, use24h: Boolean = false) = EventTimeFormatter.format(e, now, zone, use24h, Locale.US)
        assertEquals("3:30 PM", format(event("A", at(15, 30), at(16))))
        assertEquals("15:30", format(event("A", at(15, 30), at(16)), use24h = true))
        assertEquals("Now · until 11:00 AM", format(event("A", at(9), at(11))))
        assertEquals("Tomorrow 9:00 AM", format(event("A", at(9, day = 21), at(10, day = 21))))
        assertEquals("Thu 9:00 AM", format(event("A", at(9, day = 22), at(10, day = 22))))
        assertEquals("Today", format(event("A", at(0), at(0, day = 21), allDay = true)))
        assertEquals("Tomorrow", format(event("A", at(0, day = 21), at(0, day = 22), allDay = true)))
    }
}
