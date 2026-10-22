package com.sassoapps.widgets.widgets.today

import com.sassoapps.widgets.core.apps.LaunchTarget
import com.sassoapps.widgets.core.location.WeatherLocation
import com.sassoapps.widgets.core.style.FontChoice
import com.sassoapps.widgets.core.style.WidgetStyle
import com.sassoapps.widgets.core.weather.TemperatureUnit
import kotlinx.serialization.Serializable

/** Everything the user can customize on one Today widget. */
@Serializable
data class TodayConfig(
    val layout: TodayLayout = TodayLayout.SPLIT,
    val hidden: Set<TodaySection> = emptySet(),
    val clock: ClockConfig = ClockConfig(),
    val event: EventConfig = EventConfig(),
    val weather: WeatherConfig = WeatherConfig(),
    val style: WidgetStyle = WidgetStyle(),
) {
    fun isVisible(section: TodaySection) = section !in hidden
}

@Serializable
enum class TodaySection(val label: String) { CLOCK("Clock"), EVENT("Calendar events"), WEATHER("Weather") }

/** Each layout fixes where everything goes and how it's aligned. */
@Serializable
enum class TodayLayout(val label: String, val description: String) {
    SPLIT("Split", "Clock and today's weather share a row; events and the forecast sit together below. Best at 4×2 or taller."),
    STACKED("Stacked", "A large centered clock with weather below, and events on their own card. Best at 4×3."),
    COMPACT("Compact", "Clock and today's weather in one row, events underneath, no forecast. Best at 4×2 or wider."),
}

@Serializable
enum class HourFormat(val label: String) { SYSTEM("System"), H12("12-hour"), H24("24-hour") }

@Serializable
enum class DateFormat(val label: String, /** Locale-independent skeleton; each locale orders it its own way. */ val skeleton: String) {
    WEEKDAY_MONTH_DAY("Tue, Oct 20", "EEEMMMd"),
    FULL("Tuesday, October 20", "EEEEMMMMd"),
    MONTH_DAY("Oct 20", "MMMd"),
    WEEKDAY("Tuesday", "EEEE"),
    NUMERIC("10/20", "Md"),
}

@Serializable
data class ClockConfig(
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    val leadingZero: Boolean = false,
    val showAmPm: Boolean = true,
    val showDate: Boolean = true,
    val dateFormat: DateFormat = DateFormat.WEEKDAY_MONTH_DAY,
    /** Sizes the time to fill the space the layout gives it. */
    val autoSize: Boolean = true,
    /** Used when [autoSize] is off. */
    val timeSizeSp: Int = 56,
    val dateSizeSp: Int = 16,
    val font: FontChoice = FontChoice.SANS_LIGHT,
    /** Default opens the clock app's alarms. */
    val tapTarget: LaunchTarget = LaunchTarget.Default,
)

@Serializable
data class EventConfig(
    /** Calendars to read from. Empty means all of them. */
    val calendarIds: Set<Long> = emptySet(),
    val lookaheadHours: Int = 24,
    val includeAllDay: Boolean = false,
    val showLocation: Boolean = true,
    val showCalendarColor: Boolean = true,
    /** Base size; the title, time and label scale from it. */
    val textSizeSp: Int = 15,
    /** When true, tapping opens the event itself (like Pixel), in [tapTarget] if one is chosen; otherwise [tapTarget] is just launched. */
    val openEventOnTap: Boolean = true,
    /** Default opens the default calendar app. */
    val tapTarget: LaunchTarget = LaunchTarget.Default,
)

@Serializable
enum class ForecastKind(val label: String) { HOURLY("Hourly"), DAILY("Daily") }

@Serializable
data class WeatherConfig(
    val location: WeatherLocation = WeatherLocation.Device,
    val unit: TemperatureUnit = TemperatureUnit.SYSTEM,
    val showPlaceName: Boolean = true,
    val showCondition: Boolean = true,
    val showHighLow: Boolean = true,
    /** Which forecast shows first; the user can switch between hourly and daily on the widget. */
    val forecast: ForecastKind = ForecastKind.HOURLY,
    /** 0 hides the forecast. Fewer are shown if the widget is too narrow. */
    val forecastCount: Int = 3,
    /** Base size; the temperature, icon and labels scale from it. */
    val textSizeSp: Int = 15,
    /** Default opens this widget's settings. */
    val tapTarget: LaunchTarget = LaunchTarget.Default,
)
