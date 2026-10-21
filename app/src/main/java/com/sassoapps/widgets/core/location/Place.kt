package com.sassoapps.widgets.core.location

import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Place(val name: String, val latitude: Double, val longitude: Double) {
    /** Rounded to about 1 km, so nearby lookups share a cache entry. */
    val cacheKey: String get() = String.format(Locale.US, "%.2f,%.2f", latitude, longitude)
}

/** Where a widget gets its weather for. */
@Serializable
sealed interface WeatherLocation {
    /** The device's last known location, refreshed whenever the app is opened. */
    @Serializable
    @SerialName("device")
    data object Device : WeatherLocation

    @Serializable
    @SerialName("fixed")
    data class Fixed(val place: Place) : WeatherLocation
}
