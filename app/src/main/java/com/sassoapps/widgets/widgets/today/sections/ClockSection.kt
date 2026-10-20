package com.sassoapps.widgets.widgets.today.sections

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import androidx.glance.GlanceModifier
import com.sassoapps.widgets.R
import com.sassoapps.widgets.core.style.DayNight
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.widgets.today.ClockConfig
import com.sassoapps.widgets.widgets.today.HourFormat

/** TextClock format strings (12-hour-system pattern, 24-hour-system pattern). */
object ClockFormats {

    fun time(config: ClockConfig): Pair<String, String> {
        val h12 = (if (config.leadingZero) "hh" else "h") + ":mm" + (if (config.showAmPm) " a" else "")
        val h24 = (if (config.leadingZero) "HH" else "H") + ":mm"
        return when (config.hourFormat) {
            HourFormat.SYSTEM -> h12 to h24
            HourFormat.H12 -> h12 to h12
            HourFormat.H24 -> h24 to h24
        }
    }

    fun date(config: ClockConfig): Pair<String, String> = config.dateFormat.pattern to config.dateFormat.pattern
}

/**
 * The time and date, drawn with Android's TextClock so they tick every minute on their own,
 * without the app waking up to refresh the widget.
 */
@Composable
fun ClockSection(config: ClockConfig, palette: WidgetPalette, compact: Boolean, horizontalAlignment: Alignment.Horizontal) {
    val context = LocalContext.current
    val time = textClock(context, config.font, ClockFormats.time(config), timeSize(config, compact), palette.text)
    val date = if (config.showDate) {
        textClock(context, config.font, ClockFormats.date(config), config.dateSizeSp, palette.secondaryText)
    } else {
        null
    }

    if (compact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AndroidRemoteViews(time)
            if (date != null) {
                Spacer(GlanceModifier.width(8.dp))
                AndroidRemoteViews(date)
            }
        }
    } else {
        Column(horizontalAlignment = horizontalAlignment) {
            AndroidRemoteViews(time)
            if (date != null) AndroidRemoteViews(date)
        }
    }
}

/** A single line in compact layout can't fit a giant clock. */
private fun timeSize(config: ClockConfig, compact: Boolean) =
    if (compact) minOf(config.timeSizeSp, COMPACT_MAX_TIME_SP) else config.timeSizeSp

private fun textClock(
    context: Context,
    font: FontChoice,
    formats: Pair<String, String>,
    sizeSp: Int,
    color: DayNight,
): RemoteViews = RemoteViews(context.packageName, font.clockLayout).apply {
    setCharSequence(R.id.clock, "setFormat12Hour", formats.first)
    setCharSequence(R.id.clock, "setFormat24Hour", formats.second)
    setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
    setColorInt(R.id.clock, "setTextColor", color.day, color.night)
}

/** RemoteViews can't switch typefaces at runtime, so each font has its own TextClock layout. */
private val FontChoice.clockLayout: Int
    get() = when (this) {
        FontChoice.SANS -> R.layout.clock_sans
        FontChoice.SANS_LIGHT -> R.layout.clock_sans_light
        FontChoice.SANS_THIN -> R.layout.clock_sans_thin
        FontChoice.SANS_MEDIUM -> R.layout.clock_sans_medium
        FontChoice.CONDENSED -> R.layout.clock_condensed
        FontChoice.SERIF -> R.layout.clock_serif
        FontChoice.MONO -> R.layout.clock_mono
    }

private const val COMPACT_MAX_TIME_SP = 28
