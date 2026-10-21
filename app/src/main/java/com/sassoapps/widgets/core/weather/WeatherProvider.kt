package com.sassoapps.widgets.core.weather

/** A weather data source. Swap implementations to change APIs without touching the widgets. */
interface WeatherProvider {
    /** Display credit required by the provider's license, shown in the app's About section. */
    val attribution: String

    suspend fun fetch(latitude: Double, longitude: Double): WeatherData
}
