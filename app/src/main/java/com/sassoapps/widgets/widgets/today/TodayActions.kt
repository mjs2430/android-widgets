package com.sassoapps.widgets.widgets.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.core.net.toUri
import com.sassoapps.widgets.MainActivity
import com.sassoapps.widgets.core.apps.LaunchTarget
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
        event != null && config.event.openEventOnTap -> viewEvent(context, config.event.tapTarget, event)
        else -> openCalendar(context, config.event.tapTarget, event?.begin ?: data.now)
    }

    /**
     * Opens [event] in the chosen app, skipping the system chooser. Falls back to the app's launcher
     * entry when it can't show a single event, and to the plain intent when no app was chosen.
     */
    private fun viewEvent(context: Context, target: LaunchTarget, event: CalendarEvent): Intent? =
        inApp(context, target, CalendarRepository.viewEventIntent(event))

    /** Opens the calendar at [time] (the event's start), so it doesn't resume wherever it was last left. */
    private fun openCalendar(context: Context, target: LaunchTarget, time: Long): Intent? =
        inApp(context, target, CalendarRepository.openCalendarIntent(time))

    /** Sends [view] straight to the chosen app, or launches that app if it can't handle [view]. */
    private fun inApp(context: Context, target: LaunchTarget, view: Intent): Intent? = when (target) {
        LaunchTarget.Default -> view
        LaunchTarget.None -> null
        is LaunchTarget.App -> {
            val direct = Intent(view).setPackage(target.packageName)
            if (direct.resolveActivity(context.packageManager) != null) direct else target.toIntent(context) { view }
        }
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
