package com.sassoapps.widgets.core.weather

import androidx.core.text.util.LocalePreferences
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
