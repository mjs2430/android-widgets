package com.sassoapps.widgets.widgets.today.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import com.sassoapps.widgets.core.location.Place
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.core.weather.WeatherData
import com.sassoapps.widgets.core.weather.currentAt
import com.sassoapps.widgets.core.weather.hourlyEvery
import com.sassoapps.widgets.widgets.today.ForecastKind
import com.sassoapps.widgets.widgets.today.HighLowLabels
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.DETAIL_SCALE
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.FORECAST_ICON_SCALE
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.HERO_SCALE
import com.sassoapps.widgets.widgets.today.TodayMetrics.Companion.SMALL_SCALE
import com.sassoapps.widgets.widgets.today.WeatherConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Everything the weather views need besides the forecast itself. */
data class WeatherLook(
    val config: WeatherConfig,
    val palette: WidgetPalette,
    val font: FontChoice,
    val use24h: Boolean,
)

/**
 * "Raleigh · Sunny": where the weather is for, and what it's doing. The condition is dropped when the
 * line wouldn't fit in [width] (dp), rather than being cut off mid-word.
 */
@Composable
fun WeatherHeader(weather: WeatherData, place: Place?, now: Long, look: WeatherLook, width: Float = Float.MAX_VALUE) {
    // Just the city; the region rarely fits.
    val city = place?.name?.substringBefore(',')?.takeIf { look.config.showPlaceName }
    val condition = weather.currentAt(now).condition.label.takeIf { look.config.showCondition }
    val fontScale = LocalContext.current.resources.configuration.fontScale
    val full = listOfNotNull(city, condition).joinToString(" · ")
    val fits = full.length * look.config.textSizeSp * SMALL_SCALE * fontScale * HEADER_CHAR_EM <= width
    val parts = if (fits || city == null) listOfNotNull(city, condition) else listOf(city)
    if (parts.isNotEmpty()) {
        WidgetText(parts.joinToString(" · "), look.palette.text, look.config.textSizeSp * SMALL_SCALE, look.font)
    }
}

/**
 * A large icon and temperature, with today's high and low stacked beside them.
 * [labels] shortens or drops the high and low when space is tight.
 */
@Composable
fun WeatherHero(weather: WeatherData, now: Long, look: WeatherLook, labels: HighLowLabels = HighLowLabels.FULL) {
    val size = look.config.textSizeSp
    val unit = look.config.unit
    val current = weather.currentAt(now)
    val today = weather.daily.firstOrNull()?.takeIf { look.config.showHighLow && labels != HighLowLabels.NONE }
    Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetText(current.condition.icon(current.isNight), look.palette.text, size * HERO_SCALE * ICON_TO_TEXT, look.font)
        Spacer(GlanceModifier.width(8.dp))
        WidgetText(unit.format(current.tempC), look.palette.text, size * HERO_SCALE, look.font)
        if (today != null) {
            Spacer(GlanceModifier.width(10.dp))
            Column {
                val (high, low) = if (labels == HighLowLabels.SHORT) "H" to "L" else "High" to "Low"
                WidgetText("$high ${unit.format(today.highC)}", look.palette.text, size * SMALL_SCALE, look.font)
                WidgetText("$low ${unit.format(today.lowC)}", look.palette.text, size * SMALL_SCALE, look.font)
            }
        }
    }
}

/** Shown in place of the weather until there's a location and a forecast. */
@Composable
fun WeatherPlaceholder(place: Place?, look: WeatherLook) {
    val text = if (place == null) "Tap to set a weather location" else "Loading weather…"
    WidgetText(text, look.palette.secondaryText, look.config.textSizeSp * SMALL_SCALE, look.font)
}

/** A row of hourly or daily forecasts: label, icon and temperature in fixed-width columns. */
@Composable
fun ForecastRow(weather: WeatherData, now: Long, kind: ForecastKind, count: Int, itemWidth: Float, look: WeatherLook) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val unit = look.config.unit
    val items = when (kind) {
        ForecastKind.HOURLY -> {
            val format = DateTimeFormatter.ofPattern(if (look.use24h) "HH:mm" else "h a", locale)
            weather.hourlyEvery(now, HOURLY_STEP_HOURS, count, ZoneId.systemDefault()).map {
                ForecastItem(
                    label = Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).format(format),
                    icon = it.condition.icon(it.isNight),
                    value = unit.format(it.tempC),
                )
            }
        }
        ForecastKind.DAILY -> {
            val format = DateTimeFormatter.ofPattern("EEE", locale)
            // Today's high and low are already beside the temperature, so the daily forecast starts tomorrow.
            val today = LocalDate.now()
            weather.daily.filter { LocalDate.parse(it.date) > today }.take(count).map { day ->
                ForecastItem(
                    label = LocalDate.parse(day.date).format(format),
                    icon = day.condition.icon(isNight = false),
                    value = "${unit.format(day.highC)} ${unit.format(day.lowC)}",
                )
            }
        }
    }
    val size = look.config.textSizeSp
    Row {
        items.forEach { item ->
            Column(GlanceModifier.width(itemWidth.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                WidgetText(item.label, look.palette.text, size * SMALL_SCALE, look.font)
                WidgetText(item.icon, look.palette.text, size * FORECAST_ICON_SCALE, look.font)
                WidgetText(item.value, look.palette.text, size * DETAIL_SCALE, look.font)
            }
        }
    }
}

/** The hourly forecast shows every second hour, so it spans more of the day in the same width. */
private const val HOURLY_STEP_HOURS = 2

private data class ForecastItem(val label: String, val icon: String, val value: String)

/** Average glyph width of the header text, in em. */
private const val HEADER_CHAR_EM = 0.52f

/** Emoji render a little larger than text at the same size; scale them to match the digits. */
private const val ICON_TO_TEXT = 0.82f
