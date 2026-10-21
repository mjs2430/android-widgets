package com.sassoapps.widgets.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationTest {

    @Test
    fun `reads the durations the calendar provider stores`() {
        assertEquals(3_600_000L, parseDurationMillis("P3600S"))
        assertEquals(3_600_000L, parseDurationMillis("PT1H"))
        assertEquals(5_400_000L, parseDurationMillis("PT1H30M"))
        assertEquals(86_400_000L, parseDurationMillis("P1D"))
        assertEquals(604_800_000L, parseDurationMillis("P1W"))
    }

    @Test
    fun `no usable duration is null`() {
        assertNull(parseDurationMillis(null))
        assertNull(parseDurationMillis(""))
        assertNull(parseDurationMillis("P0S"))
        assertNull(parseDurationMillis("soon"))
    }
}
