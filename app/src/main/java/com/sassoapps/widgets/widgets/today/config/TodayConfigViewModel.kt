package com.sassoapps.widgets.widgets.today.config

import android.app.Application
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sassoapps.widgets.appContainer
import com.sassoapps.widgets.core.calendar.CalendarInfo
import com.sassoapps.widgets.core.location.Place
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.TodayData
import com.sassoapps.widgets.widgets.today.TodayWidget
import com.sassoapps.widgets.widgets.today.work.TodayScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the config being edited; nothing is saved until [save]. */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayConfigViewModel(app: Application, val appWidgetId: Int) : AndroidViewModel(app) {

    private val container = app.appContainer

    /** Null until the saved config has loaded. */
    private val _config = MutableStateFlow<TodayConfig?>(null)
    val config: StateFlow<TodayConfig?> = _config.asStateFlow()

    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()

    /** Live data for the preview. Only reloads when a setting that affects the data changes. */
    val data: StateFlow<TodayData?> = combine(
        _config.filterNotNull().distinctUntilChanged { a, b -> a.dataInputs() == b.dataInputs() },
        container.weather.changes,
        container.location.changes,
    ) { config, _, _ -> config }
        .mapLatest { config ->
            TodayData.load(container, config).also { data ->
                // Fetch weather for a newly chosen place; the cache update then reloads this flow.
                data.place?.let { if (container.weather.needsRefresh(it)) container.weather.refresh(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            _config.value = container.todayConfigs.get(appWidgetId)
            loadCalendars()
        }
    }

    fun update(transform: (TodayConfig) -> TodayConfig) {
        _config.update { it?.let(transform) }
    }

    fun loadCalendars() {
        viewModelScope.launch { _calendars.value = container.calendar.calendars() }
    }

    suspend fun searchPlaces(query: String): List<Place> = container.location.search(query)

    fun refreshDeviceLocation() {
        viewModelScope.launch {
            container.location.refreshDevicePlace()?.let { container.weather.refresh(it) }
        }
    }

    /** Saves the config and redraws the widget on the home screen. */
    suspend fun save() {
        val config = _config.value ?: return
        val context = getApplication<Application>()
        container.todayConfigs.set(appWidgetId, config)
        runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }
            .getOrNull()
            ?.let { TodayWidget().update(context, it) }
        TodayScheduler.ensureScheduled(context)
    }
}

/** The parts of the config that change what data is loaded (as opposed to how it looks). */
private fun TodayConfig.dataInputs() = listOf(
    hidden,
    event.calendarIds,
    event.lookaheadHours,
    event.includeAllDay,
    weather.location,
)
