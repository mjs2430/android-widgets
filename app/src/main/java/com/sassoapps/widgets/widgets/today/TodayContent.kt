package com.sassoapps.widgets.widgets.today

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.sassoapps.widgets.core.style.palette
import com.sassoapps.widgets.core.style.widgetBackground
import com.sassoapps.widgets.widgets.today.sections.ClockSection
import com.sassoapps.widgets.widgets.today.sections.EventSection
import com.sassoapps.widgets.widgets.today.sections.WeatherSection

/** The whole Today widget. Shared by the home screen widget and the in-app live preview. */
@Composable
fun TodayContent(config: TodayConfig, data: TodayData, appWidgetId: Int) {
    val context = LocalContext.current
    val style = config.style
    val palette = style.palette(context)
    val height = LocalSize.current.height
    val horizontal = when (config.alignment) {
        TodayAlignment.START -> Alignment.Horizontal.Start
        TodayAlignment.CENTER -> Alignment.Horizontal.CenterHorizontally
    }
    val use24h = when (config.clock.hourFormat) {
        HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        HourFormat.H12 -> false
        HourFormat.H24 -> true
    }
    // An empty calendar hides the event section, the way At a Glance does.
    val sections = config.visibleSections.filter { it != TodaySection.EVENT || data.event != null || !data.hasCalendarPermission }

    @Composable
    fun Section(section: TodaySection, compact: Boolean, maxStrips: Int = 0) {
        val intent = when (section) {
            TodaySection.CLOCK -> TodayActions.clock(context, config)
            TodaySection.EVENT -> TodayActions.event(context, config, data)
            TodaySection.WEATHER -> TodayActions.weather(context, config, data, appWidgetId)
        }
        Box(GlanceModifier.launch(intent)) {
            when (section) {
                TodaySection.CLOCK -> ClockSection(config.clock, palette, compact, horizontal)
                TodaySection.EVENT -> EventSection(
                    data.event, data.hasCalendarPermission, data.now, use24h, config.event, palette, style.fontFamily, compact,
                )
                TodaySection.WEATHER -> WeatherSection(
                    data.weather, data.place, use24h, config.weather, palette, style.fontFamily, maxStrips, horizontal,
                )
            }
        }
    }

    val maxStrips = when {
        height >= TWO_STRIPS_MIN_HEIGHT.dp -> 2
        height >= ONE_STRIP_MIN_HEIGHT.dp -> 1
        else -> 0
    }

    Box(
        modifier = GlanceModifier.fillMaxSize().widgetBackground(style, palette).padding(style.paddingDp.dp),
        contentAlignment = Alignment(horizontal, Alignment.Vertical.CenterVertically),
    ) {
        when (config.layout) {
            TodayLayout.STACKED -> Column(horizontalAlignment = horizontal) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) Spacer(GlanceModifier.height(6.dp))
                    Section(section, compact = false, maxStrips = maxStrips)
                }
            }

            TodayLayout.SIDE_BY_SIDE -> Row(verticalAlignment = Alignment.CenterVertically) {
                sections.firstOrNull()?.let { Section(it, compact = false, maxStrips = maxStrips) }
                if (sections.size > 1) {
                    Spacer(GlanceModifier.width(16.dp))
                    Column {
                        sections.drop(1).forEachIndexed { index, section ->
                            if (index > 0) Spacer(GlanceModifier.height(6.dp))
                            Section(section, compact = false, maxStrips = maxStrips)
                        }
                    }
                }
            }

            TodayLayout.COMPACT -> Row(verticalAlignment = Alignment.CenterVertically) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) Spacer(GlanceModifier.width(14.dp))
                    Section(section, compact = true)
                }
            }
        }
    }
}

private fun GlanceModifier.launch(intent: Intent?): GlanceModifier =
    if (intent != null) clickable(actionStartActivity(intent)) else this

private const val ONE_STRIP_MIN_HEIGHT = 150
private const val TWO_STRIPS_MIN_HEIGHT = 230
