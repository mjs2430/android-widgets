package com.sassoapps.widgets.widgets.today

import com.sassoapps.widgets.core.apps.LaunchTarget
import com.sassoapps.widgets.core.config.ConfigJson
import com.sassoapps.widgets.core.location.Place
import com.sassoapps.widgets.core.location.WeatherLocation
import com.sassoapps.widgets.core.style.ThemePreset
import com.sassoapps.widgets.widgets.today.sections.ClockFormats
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayConfigTest {

    @Test
    fun `config survives a save and load round trip`() {
        val config = TodayConfig(
            layout = TodayLayout.COMPACT,
            hidden = setOf(TodaySection.CLOCK),
            clock = ClockConfig(tapTarget = LaunchTarget.App("com.example.clock", "Clock")),
            event = EventConfig(calendarIds = setOf(1L, 4L), tapTarget = LaunchTarget.None),
            weather = WeatherConfig(location = WeatherLocation.Fixed(Place("Oslo", 59.91, 10.75)), forecast = ForecastKind.DAILY, forecastCount = 4),
            style = TodayConfig().style.copy(theme = ThemePreset.LIQUID_GLASS, opacity = 0.5f),
        )
        val json = ConfigJson.encodeToString(TodayConfig.serializer(), config)
        assertEquals(config, ConfigJson.decodeFromString(TodayConfig.serializer(), json))
    }

    @Test
    fun `unknown fields and values from other versions fall back to defaults`() {
        val json = """{"layout":"SOMETHING_NEW","futureField":true,"clock":{"timeSizeSp":40}}"""
        val config = ConfigJson.decodeFromString(TodayConfig.serializer(), json)
        assertEquals(TodayLayout.SPLIT, config.layout)
        assertEquals(40, config.clock.timeSizeSp)
    }

    @Test
    fun `clock patterns follow hour format options`() {
        assertEquals("h:mm a" to "H:mm", ClockFormats.time(ClockConfig()))
        assertEquals("h:mm" to "H:mm", ClockFormats.time(ClockConfig(showAmPm = false)))
        assertEquals("hh:mm a" to "hh:mm a", ClockFormats.time(ClockConfig(hourFormat = HourFormat.H12, leadingZero = true, showAmPm = true)))
        assertEquals("H:mm" to "H:mm", ClockFormats.time(ClockConfig(hourFormat = HourFormat.H24)))
    }
}
