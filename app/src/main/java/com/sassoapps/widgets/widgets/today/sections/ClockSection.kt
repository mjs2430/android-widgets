package com.sassoapps.widgets.widgets.today.sections

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.Gravity
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.sassoapps.widgets.R
import com.sassoapps.widgets.core.style.DayNight
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.widgets.today.ClockConfig
import com.sassoapps.widgets.widgets.today.HourFormat
import com.sassoapps.widgets.widgets.today.TodayMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    /** The date in the user's locale, e.g. "Tue, Oct 20" (US) or "Di., 20. Okt." (German). */
    fun date(config: ClockConfig, now: Long, locale: Locale): String {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, config.dateFormat.skeleton)
        return SimpleDateFormat(pattern, locale).format(Date(now))
    }
}

/**
 * The time, drawn by an Android TextClock so it ticks every minute without waking the app.
 * Glance gives the TextClock the full width of its container, so place it where it has room;
 * [align] positions the digits within that width.
 */
@Composable
fun TimeText(config: ClockConfig, sizeSp: Int, palette: WidgetPalette, align: Alignment.Horizontal) {
    val context = LocalContext.current
    // Without an explicit height, the TextClock's container can end up shorter than the text.
    val height = sizeSp * context.resources.configuration.fontScale * TodayMetrics.TIME_LINE_HEIGHT
    AndroidRemoteViews(
        textClock(context, config, sizeSp, palette.text, align),
        GlanceModifier.fillMaxWidth().height(height.dp),
    )
}

/** Today's date in the user's chosen format. Redrawn at midnight by the widget's refresh alarm. */
@Composable
fun DateText(config: ClockConfig, now: Long, sizeSp: Number, palette: WidgetPalette) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    WidgetText(ClockFormats.date(config, now, locale), palette.secondaryText, sizeSp, config.font)
}

private fun textClock(
    context: Context,
    config: ClockConfig,
    sizeSp: Int,
    color: DayNight,
    align: Alignment.Horizontal,
): RemoteViews = RemoteViews(context.packageName, config.font.clockLayout).apply {
    val centered = align == Alignment.Horizontal.CenterHorizontally
    val (full12, full24) = ClockFormats.time(config)
    // The AM/PM is its own, smaller TextClock; it's empty (so takes no room) in 24-hour mode.
    fun digits(format: String) = format.removeSuffix(AM_PM)
    fun amPm(format: String) = if (format.endsWith(AM_PM)) AM_PM else ""

    setCharSequence(R.id.clock, "setFormat12Hour", digits(full12))
    setCharSequence(R.id.clock, "setFormat24Hour", digits(full24))
    setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
    setColorInt(R.id.clock, "setTextColor", color.day, color.night)

    for (id in listOf(R.id.clock_lead, R.id.clock_trail)) {
        setCharSequence(id, "setFormat12Hour", amPm(full12))
        setCharSequence(id, "setFormat24Hour", amPm(full24))
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sizeSp * AM_PM_SCALE)
    }
    setColorInt(R.id.clock_trail, "setTextColor", color.day, color.night)
    // The lead is an invisible copy of the trail, so a centered time's digits sit in the middle.
    setColorInt(R.id.clock_lead, "setTextColor", Color.TRANSPARENT, Color.TRANSPARENT)
    setViewVisibility(R.id.clock_lead, if (centered) View.VISIBLE else View.GONE)
    setViewVisibility(R.id.clock_fill, if (centered) View.GONE else View.VISIBLE)
}

internal val Alignment.Horizontal.gravity: Int
    get() = when (this) {
        Alignment.Horizontal.CenterHorizontally -> Gravity.CENTER_HORIZONTAL
        Alignment.Horizontal.End -> Gravity.END
        else -> Gravity.START
    }

/** RemoteViews can't switch typefaces at runtime, so each font has its own clock layout. */
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

private const val AM_PM_SCALE = 0.42f

/** The 12-hour pattern's AM/PM, with the space that separates it from the digits. */
private const val AM_PM = " a"
