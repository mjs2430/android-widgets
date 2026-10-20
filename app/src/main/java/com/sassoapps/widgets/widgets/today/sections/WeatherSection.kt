package com.sassoapps.widgets.widgets.today.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.sassoapps.widgets.core.location.Place
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.core.weather.WeatherData
import com.sassoapps.widgets.widgets.today.WeatherConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Current conditions on one line, plus optional hourly and daily strips.
 * [maxStrips] limits the strips to what fits at the widget's current height.
 */
@Composable
fun WeatherSection(
    weather: WeatherData?,
    place: Place?,
    use24h: Boolean,
    config: WeatherConfig,
    palette: WidgetPalette,
    font: FontChoice,
    maxStrips: Int,
    horizontalAlignment: Alignment.Horizontal,
) {
    val size = config.textSizeSp
    if (weather == null) {
        val message = if (place == null) "Tap to set a weather location" else "Loading weather…"
        WidgetText(message, palette.secondaryText, size, font)
        return
    }
    val unit = config.unit
    val locale = LocalContext.current.resources.configuration.locales[0]
    val today = weather.daily.firstOrNull()

    Column(horizontalAlignment = horizontalAlignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WidgetText(weather.current.condition.icon(weather.current.isNight), palette.text, size * ICON_SCALE, font)
            Spacer(GlanceModifier.width(6.dp))
            if (config.showCurrent) {
                WidgetText(unit.format(weather.current.tempC), palette.text, size, font, bold = true)
            }
            if (config.showCondition) {
                WidgetText("  ${weather.current.condition.label}", palette.text, size, font)
            }
            if (config.showHighLow && today != null) {
                WidgetText("  ↑${unit.format(today.highC)} ↓${unit.format(today.lowC)}", palette.secondaryText, size * SMALL_SCALE, font)
            }
        }

        var stripsLeft = maxStrips
        if (config.hourlyCount > 0 && weather.hourly.isNotEmpty() && stripsLeft-- > 0) {
            val hourFormat = DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h a", locale)
            ForecastStrip(
                items = weather.hourly.take(config.hourlyCount).map {
                    ForecastItem(
                        label = Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).format(hourFormat),
                        icon = it.condition.icon(it.isNight),
                        value = unit.format(it.tempC),
                    )
                },
                palette = palette, font = font, sizeSp = size * SMALL_SCALE,
            )
        }
        if (config.dailyCount > 0 && weather.daily.isNotEmpty() && stripsLeft-- > 0) {
            val dayFormat = DateTimeFormatter.ofPattern("EEE", locale)
            ForecastStrip(
                items = weather.daily.take(config.dailyCount).map {
                    ForecastItem(
                        label = LocalDate.parse(it.date).format(dayFormat),
                        icon = it.condition.icon(isNight = false),
                        value = "${unit.format(it.highC)} ${unit.format(it.lowC)}",
                    )
                },
                palette = palette, font = font, sizeSp = size * SMALL_SCALE,
            )
        }
    }
}

private data class ForecastItem(val label: String, val icon: String, val value: String)

@Composable
private fun ForecastStrip(items: List<ForecastItem>, palette: WidgetPalette, font: FontChoice, sizeSp: Float) {
    Spacer(GlanceModifier.height(8.dp))
    Row {
        items.forEachIndexed { index, item ->
            Column(
                modifier = GlanceModifier.padding(start = if (index == 0) 0.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WidgetText(item.label, palette.secondaryText, sizeSp, font)
                WidgetText(item.icon, palette.text, sizeSp * ICON_SCALE, font)
                WidgetText(item.value, palette.text, sizeSp, font)
            }
        }
    }
}

private const val ICON_SCALE = 1.25f
private const val SMALL_SCALE = 0.85f
