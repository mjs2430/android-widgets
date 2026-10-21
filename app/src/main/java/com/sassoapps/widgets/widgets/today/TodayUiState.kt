package com.sassoapps.widgets.widgets.today

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.sassoapps.widgets.appContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.todayUi by preferencesDataStore(name = "today_ui")

/** What the user has switched on the widget itself, as opposed to its settings. */
data class TodayUiState(
    /** Overrides the configured forecast after the user taps it; null means use the setting. */
    val forecast: ForecastKind? = null,
)

/** Per-widget [TodayUiState], kept apart from settings so switching never touches them. */
class TodayUiStore(context: Context) {
    private val dataStore = context.applicationContext.todayUi

    fun flow(appWidgetId: Int): Flow<TodayUiState> = dataStore.data.map { prefs ->
        TodayUiState(
            forecast = prefs[forecastKey(appWidgetId)]?.let { name -> ForecastKind.entries.firstOrNull { it.name == name } },
        )
    }

    suspend fun get(appWidgetId: Int): TodayUiState = flow(appWidgetId).first()

    suspend fun setForecast(appWidgetId: Int, kind: ForecastKind) {
        dataStore.edit { it[forecastKey(appWidgetId)] = kind.name }
    }

    suspend fun delete(appWidgetId: Int) {
        dataStore.edit { it.remove(forecastKey(appWidgetId)) }
    }

    private fun forecastKey(id: Int) = stringPreferencesKey("$id:forecast")
}

/** Tap on the forecast: switch between hourly and daily. */
class ToggleForecastAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val showing = parameters[Showing]?.let { name -> ForecastKind.entries.firstOrNull { it.name == name } } ?: ForecastKind.HOURLY
        context.appContainer.todayUi.setForecast(id, ForecastKind.entries.first { it != showing })
        TodayWidget().update(context, glanceId)
    }

    companion object {
        val Showing = ActionParameters.Key<String>("showing")
    }
}
