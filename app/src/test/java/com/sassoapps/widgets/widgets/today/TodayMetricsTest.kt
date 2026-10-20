package com.sassoapps.widgets.widgets.today

import com.sassoapps.widgets.core.weather.CurrentWeather
import com.sassoapps.widgets.core.weather.HourlyForecast
import com.sassoapps.widgets.core.weather.WeatherCondition
import com.sassoapps.widgets.core.weather.WeatherData
import com.sassoapps.widgets.core.weather.currentAt
import com.sassoapps.widgets.core.weather.hourlyAfter
import com.sassoapps.widgets.core.weather.hourlyEvery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class TodayMetricsTest {

    private fun metrics(config: TodayConfig = TodayConfig(), hasEvent: Boolean = true) =
        TodayMetrics(config, hasEvent, hasWeather = true, fontScale = 1f)

    @Test
    fun `split drops the location, then the forecast, before shrinking the clock below its minimum`() {
        val roomy = metrics().split(320f, 300f)
        assertEquals(EventLines(location = true), roomy.events)
        assertTrue(roomy.forecastWidth > 0f)
        val noLocation = metrics().split(320f, 180f)
        assertEquals(EventLines(location = false), noLocation.events)
        assertTrue(noLocation.forecastWidth > 0f)
        val tight = metrics().split(320f, 150f)
        assertEquals(EventLines(location = true), tight.events)
        assertEquals(0f, tight.forecastWidth, 0f)
    }

    @Test
    fun `the clock grows with the widget and stops at a fixed size when auto size is off`() {
        val small = metrics().stacked(300f, 260f, 16f).timeSp
        val large = metrics().stacked(400f, 420f, 16f).timeSp
        assertTrue(large > small)
        val fixed = TodayConfig(clock = ClockConfig(autoSize = false, timeSizeSp = 40))
        assertEquals(40, metrics(fixed).stacked(400f, 420f, 16f).timeSp)
    }

    @Test
    fun `stacked only adds the event card when it fits`() {
        assertEquals(0f, metrics().stacked(340f, 170f, 16f).eventCardHeight, 0f)
        assertTrue(metrics().stacked(340f, 320f, 16f).eventCardHeight > 0f)
        assertEquals(0f, metrics(hasEvent = false).stacked(340f, 320f, 16f).eventCardHeight, 0f)
    }

    @Test
    fun `forecast columns follow the width and the user's count`() {
        val metrics = metrics()
        val hourly = metrics.forecastItemWidth(ForecastKind.HOURLY)
        assertEquals(2, metrics.forecastCount(hourly * 2.5f, ForecastKind.HOURLY))
        assertEquals(3, metrics.forecastCount(hourly * 10, ForecastKind.HOURLY))
        assertTrue(metrics.forecastItemWidth(ForecastKind.DAILY) > hourly)
    }

    @Test
    fun `high and low labels shorten, then hide, as space gets tight`() {
        assertEquals(HighLowLabels.FULL, metrics().highLowLabels(300f))
        assertEquals(HighLowLabels.SHORT, metrics().highLowLabels(150f))
        assertEquals(HighLowLabels.NONE, metrics().highLowLabels(100f))
    }

    @Test
    fun `current weather follows the clock, not the fetch time`() {
        val hour = 60 * 60 * 1000L
        val hourly = (0..3).map { HourlyForecast(it * hour, 10.0 + it, WeatherCondition.CLEAR, false) }
        val data = WeatherData(0, 0, CurrentWeather(99.0, WeatherCondition.CLOUDY, false), hourly, emptyList())

        assertEquals(12.0, data.currentAt(2 * hour + 5).tempC, 0.0)
        assertEquals(99.0, data.currentAt(10 * hour).tempC, 0.0)
        assertEquals(listOf(13.0), data.hourlyAfter(2 * hour + 5).map { it.tempC })
    }

    @Test
    fun `the hourly forecast rounds up to the next hour, then steps every two hours`() {
        val hour = 60 * 60 * 1000L
        val hourly = (0..12).map { HourlyForecast(it * hour, it.toDouble(), WeatherCondition.CLEAR, false) }
        val data = WeatherData(0, 0, CurrentWeather(0.0, WeatherCondition.CLEAR, false), hourly, emptyList())

        // 3:43 AM: round up to 4, then 6, 8 and 10.
        assertEquals(listOf(6.0, 8.0, 10.0), data.hourlyEvery(3 * hour + 43 * 60_000, 2, 3, ZoneOffset.UTC).map { it.tempC })
        // Exactly on the hour counts as that hour.
        assertEquals(listOf(5.0, 7.0), data.hourlyEvery(3 * hour, 2, 2, ZoneOffset.UTC).map { it.tempC })
        // Hours past the end of the forecast are dropped.
        assertEquals(listOf(12.0), data.hourlyEvery(9 * hour + 1, 2, 3, ZoneOffset.UTC).map { it.tempC })
    }
}
