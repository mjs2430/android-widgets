package com.sassoapps.widgets.core.weather

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetNorwayParserTest {

    private val json = javaClass.getResource("/met_compact.json")!!.readText()
    private val now = Instant.parse("2026-10-20T11:20:00Z").toEpochMilli()
    private val data = MetNorwayParser.parse(json, now, expiresAt = now + 1000, zone = ZoneOffset.UTC)

    @Test
    fun `current weather comes from the latest entry`() {
        assertEquals(10.0, data.current.tempC, 0.0)
        assertEquals(WeatherCondition.PARTLY_CLOUDY, data.current.condition)
        assertFalse(data.current.isNight)
    }

    @Test
    fun `hourly only includes future entries with hourly symbols`() {
        assertEquals(listOf(12.5, 14.0, 6.0), data.hourly.map { it.tempC })
        assertEquals(WeatherCondition.RAIN, data.hourly[1].condition)
        assertTrue(data.hourly[2].isNight)
    }

    @Test
    fun `daily groups by local date with high, low and midday condition`() {
        assertEquals(listOf("2026-10-20", "2026-10-21"), data.daily.map { it.date })
        assertEquals(14.0, data.daily[0].highC, 0.0)
        assertEquals(6.0, data.daily[0].lowC, 0.0)
        assertEquals(WeatherCondition.CLEAR, data.daily[0].condition)
        assertEquals(WeatherCondition.SNOW, data.daily[1].condition)
    }

    @Test
    fun `symbol codes map to conditions`() {
        assertEquals(WeatherCondition.THUNDER, MetNorwayParser.condition("rainandthunder"))
        assertEquals(WeatherCondition.SLEET, MetNorwayParser.condition("lightsleetshowers_night"))
        assertEquals(WeatherCondition.HEAVY_RAIN, MetNorwayParser.condition("heavyrainshowers_day"))
        assertEquals(WeatherCondition.MOSTLY_CLEAR, MetNorwayParser.condition("fair_day"))
        assertEquals(WeatherCondition.FOG, MetNorwayParser.condition("fog"))
        assertEquals(WeatherCondition.UNKNOWN, MetNorwayParser.condition(null))
    }

    @Test
    fun `temperatures format in the chosen unit`() {
        assertEquals("72°", TemperatureUnit.FAHRENHEIT.format(22.2))
        assertEquals("22°", TemperatureUnit.CELSIUS.format(22.2))
    }
}
