package com.sassoapps.widgets.widgets.today

import com.sassoapps.widgets.AppContainer
import com.sassoapps.widgets.core.calendar.CalendarEvent
import com.sassoapps.widgets.core.calendar.NextEvent
import com.sassoapps.widgets.core.location.Place
import com.sassoapps.widgets.core.weather.CurrentWeather
import com.sassoapps.widgets.core.weather.DailyForecast
import com.sassoapps.widgets.core.weather.HourlyForecast
import com.sassoapps.widgets.core.weather.WeatherCondition
import com.sassoapps.widgets.core.weather.WeatherData
import java.time.LocalDate

/** The live data a Today widget shows, loaded fresh on every render. */
data class TodayData(
    val now: Long,
    val hasCalendarPermission: Boolean,
    /** Upcoming events, soonest first; the widget lists them one at a time to scroll through. */
    val events: List<CalendarEvent>,
    val place: Place?,
    val weather: WeatherData?,
) {
    val event: CalendarEvent? get() = events.firstOrNull()

    companion object {
        suspend fun load(container: AppContainer, config: TodayConfig, now: Long = System.currentTimeMillis()): TodayData {
            val hasCalendar = container.calendar.hasPermission()
            val events = if (hasCalendar && config.isVisible(TodaySection.EVENT)) {
                val found = container.calendar.events(config.event.calendarIds, now, now + config.event.lookaheadHours * HOUR)
                NextEvent.upcoming(found, now, config.event.includeAllDay).take(MAX_EVENTS)
            } else {
                emptyList()
            }
            val place = if (config.isVisible(TodaySection.WEATHER)) container.location.resolve(config.weather.location) else null
            val weather = place?.let { container.weather.cached(it) }
            return TodayData(now, hasCalendar, events, place, weather)
        }

        /** Example data for the catalog preview on the app's home screen. */
        fun sample(now: Long = System.currentTimeMillis()): TodayData {
            val hourly = (1..6).map {
                HourlyForecast(now + it * HOUR, 21.0 + it % 3, if (it < 4) WeatherCondition.CLEAR else WeatherCondition.PARTLY_CLOUDY, false)
            }
            val today = LocalDate.now()
            val daily = (0..4).map {
                DailyForecast(today.plusDays(it.toLong()).toString(), 24.0 - it, 14.0 - it, WeatherCondition.entries[it % 4])
            }
            return TodayData(
                now = now,
                hasCalendarPermission = true,
                events = listOf(
                    CalendarEvent(0, 0, "Meet with contractors", now + 2 * HOUR, now + 3 * HOUR, false, "20 Flintlock Rd", 0xFF4FC3F7.toInt(), false),
                ),
                place = Place("Raleigh, NC", 0.0, 0.0),
                weather = WeatherData(now, now, CurrentWeather(22.0, WeatherCondition.CLEAR, false), hourly, daily),
            )
        }

        private const val HOUR = 60 * 60 * 1000L

        /** How many upcoming events the list holds. */
        const val MAX_EVENTS = 10
    }
}
