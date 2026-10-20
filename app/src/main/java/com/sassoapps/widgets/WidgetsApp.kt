package com.sassoapps.widgets

import android.app.Application
import android.content.Context
import com.sassoapps.widgets.core.apps.AppLauncherRepository
import com.sassoapps.widgets.core.calendar.CalendarRepository
import com.sassoapps.widgets.core.config.WidgetConfigStore
import com.sassoapps.widgets.core.location.LocationRepository
import com.sassoapps.widgets.core.weather.MetNorwayProvider
import com.sassoapps.widgets.core.weather.WeatherRepository
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.TodayUiStore
import okhttp3.OkHttpClient

class WidgetsApp : Application() {
    val container by lazy { AppContainer(this) }
}

/** App-wide singletons, created on first use. Kept manual so there's no DI framework to learn. */
class AppContainer(context: Context) {
    private val httpClient = OkHttpClient()

    val apps = AppLauncherRepository(context)
    val calendar = CalendarRepository(context)
    val location = LocationRepository(context)
    val weather = WeatherRepository(context, MetNorwayProvider(httpClient, BuildConfig.WEATHER_USER_AGENT))

    val todayConfigs = WidgetConfigStore(context, "today", TodayConfig.serializer(), ::TodayConfig)
    val todayUi = TodayUiStore(context)
}

val Context.appContainer: AppContainer get() = (applicationContext as WidgetsApp).container
