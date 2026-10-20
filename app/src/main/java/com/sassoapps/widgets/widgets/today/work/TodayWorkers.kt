package com.sassoapps.widgets.widgets.today.work

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.sassoapps.widgets.appContainer
import com.sassoapps.widgets.widgets.today.TodaySection
import com.sassoapps.widgets.widgets.today.TodayWidget

/** Refreshes the forecast for every location used by a placed Today widget. */
class WeatherRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val manager = GlanceAppWidgetManager(applicationContext)
        val places = manager.getGlanceIds(TodayWidget::class.java)
            .map { container.todayConfigs.get(manager.getAppWidgetId(it)) }
            .filter { it.isVisible(TodaySection.WEATHER) }
            .mapNotNull { container.location.resolve(it.weather.location) }
            .distinctBy { it.cacheKey }

        places.forEach { container.weather.refresh(it) }
        TodayWidget().updateAll(applicationContext)
        return Result.success()
    }
}

/** Redraws Today widgets after calendar edits, then waits for the next change. */
class CalendarChangeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        TodayWidget().updateAll(applicationContext)
        // Content-URI triggers fire once; queue the next observer behind this run.
        TodayScheduler.observeCalendar(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}
