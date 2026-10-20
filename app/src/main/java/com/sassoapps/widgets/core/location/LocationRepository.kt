package com.sassoapps.widgets.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

private val Context.locationStore by preferencesDataStore(name = "location")

/**
 * City search plus the device's approximate location.
 *
 * Only foreground (coarse) location is used, so the device location is refreshed while the app
 * is open and remembered for the widgets. This avoids the background-location permission.
 */
class LocationRepository(private val context: Context) {

    private val dataStore = context.applicationContext.locationStore
    private val deviceKey = stringPreferencesKey("device_place")

    /** Emits whenever the remembered device location changes. */
    val changes: Flow<Any> get() = dataStore.data

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun resolve(location: WeatherLocation): Place? = when (location) {
        WeatherLocation.Device -> lastDevicePlace()
        is WeatherLocation.Fixed -> location.place
    }

    suspend fun lastDevicePlace(): Place? =
        dataStore.data.first()[deviceKey]?.let { runCatching { Json.decodeFromString<Place>(it) }.getOrNull() }

    /** Asks for a fresh device location. Returns the new place, or null if unavailable. */
    suspend fun refreshDevicePlace(): Place? {
        if (!hasPermission()) return null
        val location = currentLocation() ?: return null
        val name = reverseGeocode(location.latitude, location.longitude)?.let(::displayName) ?: "Current location"
        val place = Place(name, location.latitude, location.longitude)
        dataStore.edit { it[deviceKey] = Json.encodeToString(Place.serializer(), place) }
        return place
    }

    suspend fun search(query: String): List<Place> {
        if (query.isBlank() || !Geocoder.isPresent()) return emptyList()
        return geocode { geocoder, callback ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(query, MAX_RESULTS) { callback(it) }
            } else {
                @Suppress("DEPRECATION")
                callback(geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty())
            }
        }.map { Place(displayName(it), it.latitude, it.longitude) }.distinctBy { it.name }
    }

    @Suppress("MissingPermission") // Checked by refreshDevicePlace().
    private suspend fun currentLocation(): Location? {
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { manager.isProviderEnabled(it) } ?: return null
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
            }
        }
        return fresh ?: manager.getLastKnownLocation(provider)
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): Address? {
        if (!Geocoder.isPresent()) return null
        return geocode { geocoder, callback ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { callback(it) }
            } else {
                @Suppress("DEPRECATION")
                callback(geocoder.getFromLocation(lat, lon, 1).orEmpty())
            }
        }.firstOrNull()
    }

    /** Runs a Geocoder call on a background thread; any failure yields an empty list. */
    private suspend fun geocode(block: (Geocoder, (List<Address>) -> Unit) -> Unit): List<Address> =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(GEOCODE_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { cont ->
                    try {
                        block(Geocoder(context)) { if (cont.isActive) cont.resume(it) }
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(emptyList())
                    }
                }
            }.orEmpty()
        }

    private fun displayName(address: Address): String =
        listOfNotNull(address.locality ?: address.subAdminArea ?: address.featureName, address.adminArea ?: address.countryName)
            .distinct()
            .joinToString(", ")

    private companion object {
        const val MAX_RESULTS = 5
        const val LOCATION_TIMEOUT_MILLIS = 10_000L
        const val GEOCODE_TIMEOUT_MILLIS = 10_000L
    }
}
