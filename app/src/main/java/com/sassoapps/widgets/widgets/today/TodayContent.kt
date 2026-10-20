package com.sassoapps.widgets.widgets.today

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.sassoapps.widgets.core.style.WidgetPalette
import com.sassoapps.widgets.core.style.palette
import com.sassoapps.widgets.core.style.widgetBackground
import com.sassoapps.widgets.widgets.today.sections.DateText
import com.sassoapps.widgets.widgets.today.sections.EventList
import com.sassoapps.widgets.widgets.today.sections.EventLook
import com.sassoapps.widgets.widgets.today.sections.EventPlaceholder
import com.sassoapps.widgets.widgets.today.sections.ForecastRow
import com.sassoapps.widgets.widgets.today.sections.TimeText
import com.sassoapps.widgets.widgets.today.sections.WeatherHeader
import com.sassoapps.widgets.widgets.today.sections.WeatherHero
import com.sassoapps.widgets.widgets.today.sections.WeatherLook
import com.sassoapps.widgets.widgets.today.sections.WeatherPlaceholder

/** The whole Today widget. Shared by the home screen widget and the in-app live preview. */
@Composable
fun TodayContent(config: TodayConfig, data: TodayData, appWidgetId: Int, ui: TodayUiState = TodayUiState()) {
    val context = LocalContext.current
    val size = LocalSize.current
    val use24h = when (config.clock.hourFormat) {
        HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        HourFormat.H12 -> false
        HourFormat.H24 -> true
    }
    val showEvents = config.isVisible(TodaySection.EVENT) && (data.events.isNotEmpty() || !data.hasCalendarPermission)
    val showWeather = config.isVisible(TodaySection.WEATHER)
    val metrics = TodayMetrics(config, showEvents, showWeather, context.resources.configuration.fontScale)
    val padding = config.style.paddingDp.toFloat()
    val innerWidth = size.width.value - padding * 2
    val innerHeight = size.height.value - padding * 2

    val scope = TodayScope(config, data, ui, appWidgetId, config.style.palette(context), use24h, metrics, showEvents, showWeather, size, innerWidth)
    when (config.layout) {
        TodayLayout.SPLIT -> scope.Split(metrics.split(innerWidth, innerHeight))
        TodayLayout.STACKED -> scope.Stacked(metrics.stacked(size.width.value, size.height.value, padding), size)
        TodayLayout.COMPACT -> scope.Compact(metrics.compact(innerWidth, innerHeight))
    }
}

/** Everything the layouts need, so each layout reads as a description of where things go. */
private class TodayScope(
    val config: TodayConfig,
    val data: TodayData,
    val ui: TodayUiState,
    val appWidgetId: Int,
    val palette: WidgetPalette,
    val use24h: Boolean,
    val metrics: TodayMetrics,
    val showEvents: Boolean,
    val showWeather: Boolean,
    /** The widget's size, which its background is drawn to. */
    val size: DpSize,
    /** Widget width inside its padding. */
    val innerWidth: Float,
) {
    private val showClock = config.isVisible(TodaySection.CLOCK)
    private val weatherLook = WeatherLook(config.weather, palette, config.style.fontFamily, use24h)

    /** The whole widget as one rounded, themed panel. */
    private val panel: GlanceModifier
        @Composable get() = GlanceModifier.fillMaxSize().widgetBackground(config.style, palette, size).padding(config.style.paddingDp.dp)

    /** Clock on the left; today's weather over the hourly forecast on the right, as tall as the clock; events full width below. */
    @Composable
    fun Split(sizing: TodaySizing) {
        val weatherWidth = sizing.columnWidth
        Box(panel, contentAlignment = Alignment.CenterStart) {
            Column(GlanceModifier.fillMaxWidth()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    if (showClock) {
                        Box(GlanceModifier.width((innerWidth - weatherWidth).dp)) { Clock(sizing.timeSp, Alignment.Horizontal.Start, dateScale = 1f) }
                    } else {
                        Spacer(GlanceModifier.defaultWeight())
                    }
                    if (showWeather) {
                        Column(GlanceModifier.width(weatherWidth.dp), horizontalAlignment = Alignment.Horizontal.End) {
                            Tappable(TodaySection.WEATHER, GlanceModifier.fillMaxWidth()) {
                                Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.End) {
                                    WeatherSummary(Alignment.Horizontal.End, weatherWidth)
                                }
                            }
                            if (sizing.forecastWidth > 0) {
                                Spacer(GlanceModifier.height(TodayMetrics.GAP.dp))
                                Forecast(sizing.forecastWidth, Alignment.Horizontal.End)
                            }
                        }
                    }
                }
                if (showEvents) {
                    Spacer(GlanceModifier.height(TodayMetrics.GAP.dp))
                    Events(sizing.events)
                }
            }
        }
    }

    /** A big centered clock over the weather, with events on a separate card below. */
    @Composable
    fun Stacked(sizing: TodaySizing, size: DpSize) {
        val style = config.style
        val gap = if (sizing.eventCardHeight > 0) TodayMetrics.CARD_GAP else 0f
        val mainSize = DpSize(size.width, (size.height.value - sizing.eventCardHeight - gap).dp)
        Column(GlanceModifier.fillMaxSize()) {
            Box(
                GlanceModifier.fillMaxWidth().defaultWeight().widgetBackground(style, palette, mainSize).padding(style.paddingDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                    if (showClock) Clock(sizing.timeSp, Alignment.Horizontal.CenterHorizontally, dateScale = STACKED_DATE_SCALE)
                    if (showWeather) {
                        Spacer(GlanceModifier.height(TodayMetrics.GAP.dp))
                        // The forecast has its own tap, so it sits beside the weather's tap area, not inside it.
                        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Tappable(TodaySection.WEATHER, GlanceModifier.defaultWeight()) {
                                WeatherSummary(Alignment.Horizontal.Start, innerWidth - sizing.forecastWidth)
                            }
                            if (sizing.forecastWidth > 0) Forecast(sizing.forecastWidth, Alignment.Horizontal.End)
                        }
                    }
                }
            }
            if (sizing.eventCardHeight > 0) {
                val cardSize = DpSize(size.width, sizing.eventCardHeight.dp)
                Spacer(GlanceModifier.height(gap.dp))
                Box(
                    GlanceModifier.fillMaxWidth().height(cardSize.height).widgetBackground(style, palette, cardSize)
                        .padding(horizontal = style.paddingDp.dp, vertical = TodayMetrics.CARD_PADDING.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Events(sizing.events)
                }
            }
        }
    }

    /** Clock on the left, today's weather on the right; events below a divider. No forecast. */
    @Composable
    fun Compact(sizing: TodaySizing) {
        Box(panel, contentAlignment = Alignment.CenterStart) {
            Column(GlanceModifier.fillMaxWidth()) {
                ClockAndWeather(sizing)
                if (showEvents) {
                    Spacer(GlanceModifier.height(8.dp))
                    Box(GlanceModifier.fillMaxWidth().height(1.dp).background(palette.secondaryText.withAlpha(DIVIDER_ALPHA).provider)) {}
                    Spacer(GlanceModifier.height(8.dp))
                    Events(sizing.events)
                }
            }
        }
    }

    // --- Building blocks ---

    @Composable
    private fun Clock(timeSp: Int, align: Alignment.Horizontal, dateScale: Float) {
        Tappable(TodaySection.CLOCK, GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = align) {
                if (config.clock.showDate) DateText(config.clock, data.now, config.clock.dateSizeSp * dateScale, palette)
                TimeText(config.clock, timeSp, palette, align)
            }
        }
    }

    /**
     * Two rows shared by split and compact: the date beside the place and condition, then the
     * time beside the temperature. Weather is right-aligned in a column of [TodaySizing.columnWidth].
     */
    @Composable
    private fun ClockAndWeather(sizing: TodaySizing) {
        val weather = data.weather
        val weatherColumn = GlanceModifier.width(sizing.columnWidth.dp)
        val showDate = showClock && config.clock.showDate
        if (showDate || (showWeather && weather != null)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // The place and condition are often longer than the temperature below them, so
                // they take whatever width the date leaves rather than the weather column's.
                if (showDate) Tappable(TodaySection.CLOCK) {
                    DateText(config.clock, data.now, config.clock.dateSizeSp, palette)
                }
                if (showWeather && weather != null) {
                    Tappable(TodaySection.WEATHER, GlanceModifier.defaultWeight()) {
                        Box(GlanceModifier.fillMaxWidth().padding(start = TodayMetrics.GAP.dp), contentAlignment = Alignment.CenterEnd) {
                            WeatherHeader(weather, data.place, data.now, weatherLook, sizing.columnWidth)
                        }
                    }
                } else {
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.defaultWeight()) {
                if (showClock) Tappable(TodaySection.CLOCK, GlanceModifier.fillMaxWidth()) {
                    TimeText(config.clock, sizing.timeSp, palette, Alignment.Horizontal.Start)
                }
            }
            if (showWeather) {
                Tappable(TodaySection.WEATHER, weatherColumn) {
                    Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        if (weather == null) {
                            WeatherPlaceholder(data.place, weatherLook)
                        } else {
                            WeatherHero(weather, data.now, weatherLook, metrics.highLowLabels(sizing.columnWidth))
                        }
                    }
                }
            }
        }
    }

    /** Place, condition, and the big temperature, sized for [width]. */
    @Composable
    private fun WeatherSummary(align: Alignment.Horizontal, width: Float) {
        val weather = data.weather
        if (weather == null) {
            WeatherPlaceholder(data.place, weatherLook)
            return
        }
        Column(horizontalAlignment = align) {
            WeatherHeader(weather, data.place, data.now, weatherLook, width)
            WeatherHero(weather, data.now, weatherLook, metrics.highLowLabels(width))
        }
    }

    /** The forecast in [width]. Tapping it switches between hourly and daily. */
    @Composable
    private fun Forecast(width: Float, align: Alignment.Horizontal) {
        val weather = data.weather ?: return
        val kind = ui.forecast ?: config.weather.forecast
        val toggle = actionRunCallback<ToggleForecastAction>(actionParametersOf(ToggleForecastAction.Showing to kind.name))
        Box(
            GlanceModifier.width(width.dp).clickable(toggle),
            contentAlignment = Alignment(align, Alignment.Vertical.Top),
        ) {
            ForecastRow(weather, data.now, kind, metrics.forecastCount(width, kind), metrics.forecastItemWidth(kind), weatherLook)
        }
    }

    /** The upcoming events as a list one event tall that scrolls; tapping one opens it. */
    @Composable
    private fun Events(lines: EventLines) {
        val context = LocalContext.current
        val look = EventLook(config.event, palette, config.style.fontFamily, use24h, lines)
        val events = data.events
        if (events.isEmpty()) {
            Tappable(TodaySection.EVENT, GlanceModifier.fillMaxWidth()) { EventPlaceholder(data.hasCalendarPermission, look) }
            return
        }
        EventList(
            events,
            data.now,
            look,
            metrics.eventHeight(lines),
            openEvent = { GlanceModifier.launch(TodayActions.event(context, config, data, it)) },
        )
    }

    /** Wraps a section so tapping anywhere on it opens that section's target. */
    @Composable
    private fun Tappable(section: TodaySection, modifier: GlanceModifier = GlanceModifier, content: @Composable () -> Unit) {
        val context = LocalContext.current
        val intent = when (section) {
            TodaySection.CLOCK -> TodayActions.clock(context, config)
            TodaySection.EVENT -> TodayActions.event(context, config, data, data.event)
            TodaySection.WEATHER -> TodayActions.weather(context, config, data, appWidgetId)
        }
        Box(modifier.launch(intent)) { content() }
    }
}

private fun GlanceModifier.launch(intent: Intent?): GlanceModifier =
    if (intent != null) clickable(actionStartActivity(intent)) else this

/** The stacked layout's date sits above a much larger clock, so it's drawn a bit bigger. */
private const val STACKED_DATE_SCALE = 1.3f
private const val DIVIDER_ALPHA = 0.35f
