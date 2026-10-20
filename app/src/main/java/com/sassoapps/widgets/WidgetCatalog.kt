package com.sassoapps.widgets

import android.app.Activity
import android.appwidget.AppWidgetProvider
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.TodayContent
import com.sassoapps.widgets.widgets.today.TodayData
import com.sassoapps.widgets.widgets.today.TodayWidgetReceiver
import com.sassoapps.widgets.widgets.today.config.TodayConfigActivity

/** A widget the app offers. Add new widgets to [WidgetCatalog] to list them on the home screen. */
data class WidgetDefinition(
    val title: String,
    val description: String,
    val receiver: Class<out AppWidgetProvider>,
    val configActivity: Class<out Activity>,
    val preview: @GlanceComposable @Composable () -> Unit,
)

val WidgetCatalog = listOf(
    WidgetDefinition(
        title = "Today",
        description = "Clock, your next event and the weather, styled your way.",
        receiver = TodayWidgetReceiver::class.java,
        configActivity = TodayConfigActivity::class.java,
        preview = { TodayContent(TodayConfig(), TodayData.sample(), appWidgetId = 0) },
    ),
)
