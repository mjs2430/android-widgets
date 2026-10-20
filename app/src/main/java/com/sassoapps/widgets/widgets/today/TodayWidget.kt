package com.sassoapps.widgets.widgets.today

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.sassoapps.widgets.appContainer
import com.sassoapps.widgets.core.calendar.NextEvent
import com.sassoapps.widgets.widgets.today.work.TodayScheduler
import java.time.ZoneId

class TodayWidget : GlanceAppWidget() {

    /** Render at the exact size so backgrounds and forecast strips fit the real widget. */
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialConfig = container.todayConfigs.get(appWidgetId)
        val initialData = loadData(context, appWidgetId, initialConfig)

        provideContent {
            val config by container.todayConfigs.flow(appWidgetId).collectAsState(initialConfig)
            val weatherCache by container.weather.changes.collectAsState(null)
            val devicePlace by container.location.changes.collectAsState(null)
            var data by remember { mutableStateOf(initialData) }

            // Reload while the widget is open if its settings, forecast or location change.
            LaunchedEffect(config, weatherCache, devicePlace) {
                data = loadData(context, appWidgetId, config)
            }
            TodayContent(config, data, appWidgetId)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        context.appContainer.todayConfigs.delete(appWidgetId)
        TodayScheduler.cancelTick(context, appWidgetId)
    }

    private suspend fun loadData(context: Context, appWidgetId: Int, config: TodayConfig): TodayData {
        val data = TodayData.load(context.appContainer, config)
        TodayScheduler.scheduleTick(context, appWidgetId, NextEvent.nextChangeAt(data.event, data.now, ZoneId.systemDefault()))
        if (data.place != null && context.appContainer.weather.needsRefresh(data.place)) {
            TodayScheduler.refreshWeatherNow(context)
        }
        return data
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TodayScheduler.ensureScheduled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        TodayScheduler.cancelAll(context)
    }
}
