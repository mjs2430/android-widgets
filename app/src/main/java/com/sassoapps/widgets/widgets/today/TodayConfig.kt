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
    val layout: TodayLayout = TodayLayout.STACKED,
    val alignment: TodayAlignment = TodayAlignment.START,
    /** Display order; hidden sections stay in the list so their position is remembered. */
    val sections: List<SectionSlot> = TodaySection.entries.map { SectionSlot(it, visible = true) },
    val clock: ClockConfig = ClockConfig(),
    val event: EventConfig = EventConfig(),
    val weather: WeatherConfig = WeatherConfig(),
    val style: WidgetStyle = WidgetStyle(),
) {
    val visibleSections: List<TodaySection> get() = sections.filter { it.visible }.map { it.section }

    fun isVisible(section: TodaySection) = section in visibleSections
}

@Serializable
enum class TodaySection(val label: String) { CLOCK("Clock"), EVENT("Next event"), WEATHER("Weather") }

@Serializable
data class SectionSlot(val section: TodaySection, val visible: Boolean)

@Serializable
enum class TodayLayout(val label: String, val description: String) {
    STACKED("Stacked", "Sections one above another"),
    SIDE_BY_SIDE("Side by side", "First section on the left, the rest on the right"),
    COMPACT("Compact", "Everything on a single line"),
}

@Serializable
enum class TodayAlignment(val label: String) { START("Left"), CENTER("Center") }

@Serializable
enum class HourFormat(val label: String) { SYSTEM("System"), H12("12-hour"), H24("24-hour") }

@Serializable
enum class DateFormat(val label: String, val pattern: String) {
    WEEKDAY_MONTH_DAY("Tue, Oct 20", "EEE, MMM d"),
    FULL("Tuesday, October 20", "EEEE, MMMM d"),
    WEEKDAY("Tuesday", "EEEE"),
    NUMERIC("10/20", "M/d"),
}

@Serializable
data class ClockConfig(
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    val leadingZero: Boolean = false,
    val showAmPm: Boolean = false,
    val showDate: Boolean = true,
    val dateFormat: DateFormat = DateFormat.WEEKDAY_MONTH_DAY,
    val timeSizeSp: Int = 48,
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
    val showLocation: Boolean = false,
    val showCalendarColor: Boolean = true,
    val textSizeSp: Int = 15,
    /** When true, tapping opens the event itself (like Pixel); otherwise [tapTarget]. */
    val openEventOnTap: Boolean = true,
    /** Default opens the default calendar app. */
    val tapTarget: LaunchTarget = LaunchTarget.Default,
)

@Serializable
data class WeatherConfig(
    val location: WeatherLocation = WeatherLocation.Device,
    val unit: TemperatureUnit = TemperatureUnit.SYSTEM,
    val showCurrent: Boolean = true,
    val showCondition: Boolean = false,
    val showHighLow: Boolean = true,
    /** 0 hides the hourly strip. */
    val hourlyCount: Int = 0,
    /** 0 hides the daily strip. */
    val dailyCount: Int = 0,
    val textSizeSp: Int = 15,
    /** Default opens this widget's settings. */
    val tapTarget: LaunchTarget = LaunchTarget.Default,
)
