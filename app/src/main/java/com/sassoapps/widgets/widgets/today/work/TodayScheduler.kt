package com.sassoapps.widgets.widgets.today.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sassoapps.widgets.widgets.today.TodayWidgetReceiver
import java.time.Duration

/**
 * Keeps Today widgets current without polling:
 * - weather refreshes hourly (and right away when a widget has none),
 * - calendar edits trigger a redraw through a content observer,
 * - an inexact alarm redraws a widget when its event starts, ends, or the day changes.
 * The clock needs none of this; TextClock ticks by itself.
 */
object TodayScheduler {

    private const val WEATHER_PERIODIC = "today-weather-periodic"
    private const val WEATHER_NOW = "today-weather-now"
    internal const val CALENDAR_OBSERVER = "today-calendar-observer"

    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun ensureScheduled(context: Context) {
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork(
            WEATHER_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeatherRefreshWorker>(Duration.ofHours(1)).setConstraints(networkConstraint).build(),
        )
        observeCalendar(context, ExistingWorkPolicy.KEEP)
    }

    fun cancelAll(context: Context) {
        val work = WorkManager.getInstance(context)
        listOf(WEATHER_PERIODIC, WEATHER_NOW, CALENDAR_OBSERVER).forEach(work::cancelUniqueWork)
    }

    fun refreshWeatherNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WEATHER_NOW,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WeatherRefreshWorker>().setConstraints(networkConstraint).build(),
        )
    }

    /** Runs [CalendarChangeWorker] the next time any calendar data changes. */
    internal fun observeCalendar(context: Context, policy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
            .setTriggerContentUpdateDelay(Duration.ofSeconds(5))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CALENDAR_OBSERVER,
            policy,
            OneTimeWorkRequestBuilder<CalendarChangeWorker>().setConstraints(constraints).build(),
        )
    }

    /** Redraws one widget at roughly [atMillis]. Inexact and non-waking, so it costs no battery. */
    fun scheduleTick(context: Context, appWidgetId: Int, atMillis: Long) {
        context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, atMillis, tickIntent(context, appWidgetId))
    }

    fun cancelTick(context: Context, appWidgetId: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(tickIntent(context, appWidgetId))
    }

    private fun tickIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, TodayWidgetReceiver::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        return PendingIntent.getBroadcast(
            context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
