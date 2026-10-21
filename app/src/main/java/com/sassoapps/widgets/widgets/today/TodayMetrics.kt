package com.sassoapps.widgets.widgets.today

import kotlin.math.floor

enum class HighLowLabels { FULL, SHORT, NONE }

/** Whether the location line fits under the event title. */
data class EventLines(val location: Boolean)

/** Sizes chosen for one render of the widget, derived from its size and the user's settings. */
data class TodaySizing(
    val timeSp: Int,
    val events: EventLines,
    /** Width given to the forecast; 0 hides it. */
    val forecastWidth: Float,
    /** Stacked layout only: height of the separate event card, or 0 when it doesn't fit. */
    val eventCardHeight: Float = 0f,
    /** Fixed width of the weather column in split and compact. */
    val columnWidth: Float = 0f,
)

/**
 * Rough size estimates (in dp) used to fit each layout to the widget, so content is
 * scaled, added or dropped instead of being clipped. Text is measured in sp, so
 * everything is multiplied by the user's [fontScale].
 */
class TodayMetrics(
    private val config: TodayConfig,
    private val hasEvent: Boolean,
    private val hasWeather: Boolean,
    private val fontScale: Float,
) {
    private fun line(sp: Float) = sp * fontScale * LINE_HEIGHT

    private val eventSp get() = config.event.textSizeSp.toFloat()
    private val weatherSp get() = config.weather.textSizeSp.toFloat()
    private val clock get() = config.clock
    private val showClock get() = config.isVisible(TodaySection.CLOCK)

    // --- Section heights ---

    val dateHeight: Float get() = if (showClock && clock.showDate) line(clock.dateSizeSp.toFloat()) else 0f

    fun timeHeight(timeSp: Int) = if (showClock) timeSp * fontScale * TIME_LINE_HEIGHT else 0f

    /** Height of one event: the label line (with the time beside it), the title, and the location if it fits. */
    fun eventHeight(lines: EventLines, card: Boolean = false): Float {
        if (!hasEvent) return 0f
        val detail = line(eventSp * DETAIL_SCALE)
        return line(eventSp * DETAIL_SCALE) + line(eventSp * TITLE_SCALE) +
            (if (lines.location) detail else 0f) +
            if (card) CARD_PADDING * 2 else 0f
    }

    val weatherHeaderHeight: Float
        get() = if (hasWeather && (config.weather.showPlaceName || config.weather.showCondition)) line(weatherSp * SMALL_SCALE) else 0f

    val weatherHeroHeight: Float get() = if (hasWeather) line(weatherSp * HERO_SCALE) else 0f

    val forecastHeight: Float
        get() = line(weatherSp * SMALL_SCALE) + line(weatherSp * FORECAST_ICON_SCALE) + line(weatherSp * DETAIL_SCALE)

    /** How today's high and low fit beside the temperature in [width]: "High 70°", "H 70°", or not at all. */
    fun highLowLabels(width: Float): HighLowLabels {
        val em = weatherSp * fontScale
        val temperature = em * HERO_SCALE * (HERO_ICON_EM + 3 * DIGIT_EM) + HERO_GAPS
        return when {
            temperature + em * SMALL_SCALE * HIGH_LOW_EM <= width -> HighLowLabels.FULL
            temperature + em * SMALL_SCALE * SHORT_HIGH_LOW_EM <= width -> HighLowLabels.SHORT
            else -> HighLowLabels.NONE
        }
    }

    /** Width of one forecast column; daily columns hold both the high and low. */
    fun forecastItemWidth(kind: ForecastKind = config.weather.forecast): Float =
        (if (kind == ForecastKind.DAILY) DAILY_ITEM_WIDTH else HOURLY_ITEM_WIDTH) * (weatherSp / BASE_SP) * fontScale.coerceAtLeast(1f)

    // --- Fitting ---

    /**
     * Width of the time in em: four digits, a colon, and a small AM/PM. A [centered] time also
     * reserves the same space in front of the digits, so they sit in the middle (see ClockSection).
     */
    private fun timeWidthEm(centered: Boolean): Float =
        4 * DIGIT_EM + COLON_EM + if (clock.showAmPm) AM_PM_EM + if (centered) CENTERING_EM else 0f else 0f

    private fun autoTime(width: Float, height: Float, min: Int, max: Int, centered: Boolean = false): Int {
        if (!clock.autoSize) return clock.timeSizeSp
        val byWidth = width / (timeWidthEm(centered) * fontScale)
        val byHeight = height / (TIME_LINE_HEIGHT * fontScale)
        return minOf(byWidth, byHeight).toInt().coerceIn(min, max)
    }

    /** How many forecast columns of [kind] fit in [width], up to the user's chosen count. */
    fun forecastCount(width: Float, kind: ForecastKind): Int =
        floor(width / forecastItemWidth(kind)).toInt().coerceIn(0, config.weather.forecastCount)

    /** [width] if at least one forecast column fits there, otherwise 0 (hidden). */
    private fun forecastWidth(width: Float): Float =
        if (hasWeather && forecastCount(width, config.weather.forecast) > 0) width else 0f

    /** Event lines in order of what to drop first: the location. */
    private fun eventLineOptions() = listOf(
        EventLines(location = config.event.showLocation),
        EventLines(location = false),
    ).distinct()

    /**
     * The clock on the left, today's weather over the hourly forecast on the right, and the events
     * full width below. The clock block grows to the height of the weather column.
     */
    fun split(width: Float, height: Float): TodaySizing {
        val right = if (hasWeather) width * SPLIT_WEATHER_SHARE else 0f
        val forecastWidth = forecastWidth(right)

        fun weatherHeight(forecast: Boolean) =
            if (hasWeather) weatherHeaderHeight + weatherHeroHeight + if (forecast) GAP + forecastHeight else 0f else 0f

        fun roomAbove(lines: EventLines) = height - eventHeight(lines) - if (hasEvent) GAP else 0f

        /** Height the top row needs: the weather column, or at least a clock of the minimum size. */
        fun needed(forecast: Boolean) = maxOf(weatherHeight(forecast), dateHeight + timeHeight(MIN_SPLIT_TIME))

        // Drop the event location before the forecast, and the forecast before shrinking the clock.
        val candidates = (if (forecastWidth > 0) eventLineOptions().map { it to true } else emptyList()) +
            eventLineOptions().map { it to false }
        val (lines, forecast) = candidates.firstOrNull { (lines, forecast) -> roomAbove(lines) >= needed(forecast) }
            ?: (EventLines(location = false) to false)
        val rowHeight = if (hasWeather) minOf(roomAbove(lines), weatherHeight(forecast)) else roomAbove(lines)
        val timeSp = autoTime(width - right, rowHeight - dateHeight, MIN_SPLIT_TIME, MAX_SPLIT_TIME)
        return TodaySizing(timeSp, lines, if (forecast) forecastWidth else 0f, columnWidth = right)
    }

    /** A big centered clock with weather underneath, and events on their own card below. */
    fun stacked(width: Float, height: Float, padding: Float): TodaySizing {
        val inner = width - padding * 2
        val weatherRow = maxOf(weatherHeaderHeight + weatherHeroHeight, forecastHeight)
        val mainFixed = padding * 2 + dateHeight + weatherRow + GAP * 2

        val lines = eventLineOptions().firstOrNull {
            height - eventHeight(it, card = true) - CARD_GAP - mainFixed >= timeHeight(MIN_STACKED_TIME)
        }
        val card = if (lines != null) eventHeight(lines, card = true) else 0f
        val mainHeight = height - card - if (card > 0) CARD_GAP else 0f
        val timeSp = autoTime(inner, mainHeight - mainFixed, MIN_STACKED_TIME, MAX_STACKED_TIME, centered = true)
        return TodaySizing(timeSp, lines ?: EventLines(location = false), forecastWidth(inner * STACKED_FORECAST_SHARE), card)
    }

    /** Clock on the left, weather on the right (no forecast); events underneath. */
    fun compact(width: Float, height: Float): TodaySizing {
        val lines = EventLines(location = config.event.showLocation)
        val eventRow = if (hasEvent) eventHeight(lines) + DIVIDER_HEIGHT else 0f
        val clockWidth = width * COMPACT_CLOCK_SHARE
        val topLine = maxOf(dateHeight, weatherHeaderHeight)
        val timeSp = autoTime(clockWidth, height - eventRow - topLine, MIN_COMPACT_TIME, MAX_COMPACT_TIME)
        return TodaySizing(timeSp, lines, forecastWidth = 0f, columnWidth = width - clockWidth)
    }

    companion object {
        const val LINE_HEIGHT = 1.3f
        const val TIME_LINE_HEIGHT = 1.12f
        const val GAP = 6f
        const val CARD_GAP = 8f
        const val CARD_PADDING = 14f
        /** Room after each event in the list. It sits below the visible window, so it only shows while scrolling between events. */
        const val EVENT_GAP = 14f
        const val DIVIDER_HEIGHT = 17f
        const val HOURLY_ITEM_WIDTH = 46f
        const val DAILY_ITEM_WIDTH = 64f
        private const val BASE_SP = 15f

        // Text scales relative to each section's base size.
        const val LABEL_SCALE = 0.72f
        const val TITLE_SCALE = 1.35f
        const val DETAIL_SCALE = 0.82f
        const val SMALL_SCALE = 0.85f
        const val HERO_SCALE = 2.4f
        const val FORECAST_ICON_SCALE = 1.2f

        // Approximate glyph widths, in em.
        private const val DIGIT_EM = 0.56f
        private const val COLON_EM = 0.28f
        private const val AM_PM_EM = 0.62f
        /** The placeholder in front of a centered time; a little more than the AM/PM it balances. */
        private const val CENTERING_EM = 1.2f
        private const val HERO_ICON_EM = 0.95f
        private const val HIGH_LOW_EM = 4.6f
        private const val SHORT_HIGH_LOW_EM = 2.8f
        private const val HERO_GAPS = 18f

        private const val SPLIT_WEATHER_SHARE = 0.46f
        private const val STACKED_FORECAST_SHARE = 0.5f
        private const val COMPACT_CLOCK_SHARE = 0.34f

        private const val MIN_SPLIT_TIME = 32
        private const val MAX_SPLIT_TIME = 84
        private const val MIN_STACKED_TIME = 44
        private const val MAX_STACKED_TIME = 150
        private const val MIN_COMPACT_TIME = 26
        private const val MAX_COMPACT_TIME = 64
    }
}
