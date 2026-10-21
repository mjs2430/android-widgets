package com.sassoapps.widgets.widgets.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.core.net.toUri
import com.sassoapps.widgets.MainActivity
import com.sassoapps.widgets.core.apps.toIntent
import com.sassoapps.widgets.core.calendar.CalendarEvent
import com.sassoapps.widgets.core.calendar.CalendarRepository
import com.sassoapps.widgets.widgets.today.config.TodayConfigActivity

/** What each section opens when tapped. Null means the section isn't tappable. */
object TodayActions {

    fun clock(context: Context, config: TodayConfig): Intent? =
        config.clock.tapTarget.toIntent(context) { Intent(AlarmClock.ACTION_SHOW_ALARMS) }

    /** What tapping [event] opens; [event] is null when no event is showing. */
    fun event(context: Context, config: TodayConfig, data: TodayData, event: CalendarEvent?): Intent? = when {
        !data.hasCalendarPermission -> Intent(context, MainActivity::class.java)
        event != null && config.event.openEventOnTap -> CalendarRepository.viewEventIntent(event)
        else -> config.event.tapTarget.toIntent(context) { CalendarRepository.openCalendarIntent(data.now) }
    }

    fun weather(context: Context, config: TodayConfig, data: TodayData, appWidgetId: Int): Intent? =
        if (data.place == null) {
            settings(context, appWidgetId)
        } else {
            config.weather.tapTarget.toIntent(context) { settings(context, appWidgetId) }
        }

    fun settings(context: Context, appWidgetId: Int): Intent =
        Intent(context, TodayConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // PendingIntents ignore extras when comparing, so give each widget a distinct data URI.
            .setData("today://settings/$appWidgetId".toUri())
}
