package com.sassoapps.widgets.core.style

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.ContentScale
import androidx.glance.text.FontFamily
import androidx.glance.unit.ColorProvider

/** A color with separate light-mode and dark-mode values. */
data class DayNight(val day: Int, val night: Int) {
    constructor(color: Int) : this(color, color)

    val provider: ColorProvider get() = ColorProvider(day = Color(day), night = Color(night))

    fun withAlpha(alpha: Float) = DayNight(day.withAlpha(alpha), night.withAlpha(alpha))
}

/** The colors a widget draws with, after the theme preset has been applied. */
data class WidgetPalette(
    val text: DayNight,
    val secondaryText: DayNight,
    /** Flat background color, or null when the theme draws a bitmap or nothing at all. */
    val background: DayNight?,
)

fun WidgetStyle.palette(context: Context): WidgetPalette {
    if (theme == ThemePreset.MATERIAL_YOU) {
        fun color(id: Int) = context.getColor(id)
        val text = DayNight(color(android.R.color.system_neutral1_900), color(android.R.color.system_neutral1_50))
        return WidgetPalette(
            text = text,
            secondaryText = DayNight(color(android.R.color.system_neutral2_700), color(android.R.color.system_neutral2_200)),
            background = DayNight(color(android.R.color.system_accent2_50), color(android.R.color.system_neutral1_900))
                .withAlpha(opacity),
        )
    }
    return WidgetPalette(
        text = DayNight(textColor),
        secondaryText = DayNight(textColor.withAlpha(0.75f)),
        background = if (theme == ThemePreset.SOLID) DayNight(backgroundColor.withAlpha(opacity)) else null,
    )
}

val FontChoice.glanceFamily: FontFamily get() = FontFamily(family)

/**
 * Rounds the corners and paints the themed background. [size] is the area being painted:
 * the whole widget by default, or one card of it.
 */
@Composable
fun GlanceModifier.widgetBackground(style: WidgetStyle, palette: WidgetPalette, size: DpSize = LocalSize.current): GlanceModifier {
    val rounded = cornerRadius(style.cornerRadiusDp.dp)
    if (BackgroundRenderer.needsBitmap(style.theme)) {
        val scale = LocalContext.current.resources.displayMetrics.density.coerceAtMost(MAX_RENDER_SCALE)
        val bitmap = BackgroundRenderer.render(style, size.width.value, size.height.value, scale)
        return rounded.background(ImageProvider(bitmap), ContentScale.FillBounds)
    }
    return palette.background?.let { rounded.background(it.provider) } ?: rounded
}

/** Caps bitmap resolution; gradients look identical and the widget's memory use stays low. */
private const val MAX_RENDER_SCALE = 2f
