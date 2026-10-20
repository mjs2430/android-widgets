package com.sassoapps.widgets.core.weather

import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Forecasts from the Norwegian Meteorological Institute (api.met.no): free, global, and
 * licensed for commercial use with attribution. Their terms require an identifying
 * User-Agent and honoring the Expires header, which [WeatherRepository] does.
 */
class MetNorwayProvider(
    private val client: OkHttpClient,
    private val userAgent: String,
) : WeatherProvider {

    override val attribution = "Weather data from MET Norway, licensed under CC BY 4.0"

    override suspend fun fetch(latitude: Double, longitude: Double): WeatherData = withContext(Dispatchers.IO) {
        // MET Norway asks for at most 4 decimals; 2 (about 1 km) is plenty and improves their caching.
        val url = String.format(Locale.US, "$BASE_URL?lat=%.2f&lon=%.2f", latitude, longitude)
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("MET Norway returned HTTP ${response.code}")
            val now = System.currentTimeMillis()
            val expiresAt = response.headers.getDate("Expires")?.time ?: (now + DEFAULT_TTL_MILLIS)
            MetNorwayParser.parse(response.body!!.string(), now, expiresAt, ZoneId.systemDefault())
        }
    }

    private companion object {
        const val BASE_URL = "https://api.met.no/weatherapi/locationforecast/2.0/compact"
        const val DEFAULT_TTL_MILLIS = 60 * 60 * 1000L
    }
}

/** Turns a locationforecast/2.0/compact response into [WeatherData]. Pure, so it's unit tested. */
object MetNorwayParser {

    private data class Entry(val time: Long, val tempC: Double, val symbol: String?, val hasHourlySymbol: Boolean)

    private const val HOUR_MILLIS = 60 * 60 * 1000L
    private const val MAX_HOURS = 24
    private const val MAX_DAYS = 7

    fun parse(json: String, now: Long, expiresAt: Long, zone: ZoneId): WeatherData {
        val series = Json.parseToJsonElement(json).jsonObject
            .getValue("properties").jsonObject
            .getValue("timeseries").jsonArray
            .map { parseEntry(it.jsonObject) }
            .filter { it.time > now - HOUR_MILLIS }
        require(series.isNotEmpty()) { "Forecast has no upcoming entries" }

        val first = series.first()
        val current = CurrentWeather(first.tempC, condition(first.symbol), isNight(first.symbol))

        val hourly = series
            .filter { it.time > now && it.hasHourlySymbol }
            .take(MAX_HOURS)
            .map { HourlyForecast(it.time, it.tempC, condition(it.symbol), isNight(it.symbol)) }

        val daily = series
            .groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }
            .entries
            .take(MAX_DAYS)
            .map { (date, entries) ->
                // Describe the day by its midday forecast.
                val midday = entries.filter { it.symbol != null }
                    .minByOrNull { abs(Instant.ofEpochMilli(it.time).atZone(zone).hour - 12) }
                DailyForecast(
                    date = date.toString(),
                    highC = entries.maxOf { it.tempC },
                    lowC = entries.minOf { it.tempC },
                    condition = condition(midday?.symbol),
                )
            }

        return WeatherData(now, expiresAt, current, hourly, daily)
    }

    private fun parseEntry(obj: JsonObject): Entry {
        val data = obj.getValue("data").jsonObject
        fun symbolOf(period: String) = data[period]?.jsonObject
            ?.get("summary")?.jsonObject
            ?.get("symbol_code")?.jsonPrimitive?.content
        val hourSymbol = symbolOf("next_1_hours")
        return Entry(
            time = Instant.parse(obj.getValue("time").jsonPrimitive.content).toEpochMilli(),
            tempC = data.getValue("instant").jsonObject
                .getValue("details").jsonObject
                .getValue("air_temperature").jsonPrimitive.double,
            symbol = hourSymbol ?: symbolOf("next_6_hours") ?: symbolOf("next_12_hours"),
            hasHourlySymbol = hourSymbol != null,
        )
    }

    private fun isNight(symbol: String?) = symbol?.endsWith("_night") == true

    /** Maps MET symbol codes (e.g. "lightrainshowers_day") to our coarser conditions. */
    internal fun condition(symbol: String?): WeatherCondition {
        val base = symbol?.substringBefore('_') ?: return WeatherCondition.UNKNOWN
        return when {
            "thunder" in base -> WeatherCondition.THUNDER
            "sleet" in base -> WeatherCondition.SLEET
            "snow" in base -> WeatherCondition.SNOW
            base.startsWith("heavyrain") -> WeatherCondition.HEAVY_RAIN
            "rain" in base -> WeatherCondition.RAIN
            base == "fog" -> WeatherCondition.FOG
            base == "cloudy" -> WeatherCondition.CLOUDY
            base == "partlycloudy" -> WeatherCondition.PARTLY_CLOUDY
            base == "fair" -> WeatherCondition.MOSTLY_CLEAR
            base == "clearsky" -> WeatherCondition.CLEAR
            else -> WeatherCondition.UNKNOWN
        }
    }
}
