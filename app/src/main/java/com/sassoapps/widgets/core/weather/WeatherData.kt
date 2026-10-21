package com.sassoapps.widgets.core.weather

import androidx.core.text.util.LocalePreferences
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Serializable
enum class WeatherCondition(val label: String, private val dayIcon: String, private val nightIcon: String) {
    CLEAR("Clear", "☀️", "🌙"),
    MOSTLY_CLEAR("Mostly clear", "🌤️", "🌙"),
    PARTLY_CLOUDY("Partly cloudy", "⛅", "☁️"),
    CLOUDY("Cloudy", "☁️", "☁️"),
    FOG("Fog", "🌫️", "🌫️"),
    RAIN("Rain", "🌧️", "🌧️"),
    HEAVY_RAIN("Heavy rain", "🌧️", "🌧️"),
    SLEET("Sleet", "🌨️", "🌨️"),
    SNOW("Snow", "❄️", "❄️"),
    THUNDER("Thunderstorms", "⛈️", "⛈️"),
    UNKNOWN("—", "🌡️", "🌡️");

    fun icon(isNight: Boolean) = if (isNight) nightIcon else dayIcon
}

@Serializable
data class CurrentWeather(val tempC: Double, val condition: WeatherCondition, val isNight: Boolean)

@Serializable
data class HourlyForecast(val time: Long, val tempC: Double, val condition: WeatherCondition, val isNight: Boolean)

@Serializable
data class DailyForecast(
    /** ISO local date, e.g. 2026-10-20. */
    val date: String,
    val highC: Double,
    val lowC: Double,
    val condition: WeatherCondition,
)

@Serializable
data class WeatherData(
    val fetchedAt: Long,
    /** When the provider says this data should be refreshed. */
    val expiresAt: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
)

/** Conditions for the hour containing [now], so a forecast fetched earlier still shows the present. */
fun WeatherData.currentAt(now: Long): CurrentWeather =
    hourly.lastOrNull { it.time <= now && now - it.time < HOUR_MILLIS }
        ?.let { CurrentWeather(it.tempC, it.condition, it.isNight) }
        ?: current

/** Hourly forecasts that start after [now]. */
fun WeatherData.hourlyAfter(now: Long): List<HourlyForecast> = hourly.filter { it.time > now }

/**
 * [count] hourly forecasts every [stepHours] hours. The first is [stepHours] after the next full hour,
 * so at 3:43 PM with a 2-hour step you get 6, 8 and 10 PM. Hours the forecast doesn't cover are left out.
 */
fun WeatherData.hourlyEvery(now: Long, stepHours: Int, count: Int, zone: ZoneId): List<HourlyForecast> {
    val thisHour = Instant.ofEpochMilli(now).atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
    val nextHour = if (thisHour == now) now else thisHour + HOUR_MILLIS
    return (1..count).mapNotNull { step ->
        val target = nextHour + step * stepHours * HOUR_MILLIS
        // Zones off the UTC hour (e.g. +5:30) land between forecast entries, so take the nearest within half an hour.
        hourly.minByOrNull { abs(it.time - target) }?.takeIf { abs(it.time - target) <= HOUR_MILLIS / 2 }
    }
}

private const val HOUR_MILLIS = 60 * 60 * 1000L

@Serializable
enum class TemperatureUnit(val label: String) {
    SYSTEM("System"),
    FAHRENHEIT("°F"),
    CELSIUS("°C");

    fun resolve(): TemperatureUnit = when (this) {
        SYSTEM -> if (LocalePreferences.getTemperatureUnit() == LocalePreferences.TemperatureUnit.FAHRENHEIT) FAHRENHEIT else CELSIUS
        else -> this
    }

    /** Formats a Celsius temperature as e.g. "72°" in this unit. */
    fun format(tempC: Double): String {
        val value = if (resolve() == FAHRENHEIT) tempC * 9 / 5 + 32 else tempC
        return "${value.roundToInt()}°"
    }
}
