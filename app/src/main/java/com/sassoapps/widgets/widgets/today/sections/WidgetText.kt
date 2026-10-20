package com.sassoapps.widgets.widgets.today.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sassoapps.widgets.core.style.DayNight
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.glanceFamily

/** Single-style widget text, so every section sizes and colors text the same way. */
@Composable
internal fun WidgetText(
    text: String,
    color: DayNight,
    sizeSp: Number,
    font: FontChoice,
    modifier: GlanceModifier = GlanceModifier,
    bold: Boolean = false,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        style = TextStyle(
            color = color.provider,
            fontSize = sizeSp.toFloat().sp,
            fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
            fontFamily = font.glanceFamily,
        ),
    )
}
