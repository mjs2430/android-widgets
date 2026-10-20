package com.sassoapps.widgets.core.weather

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sassoapps.widgets.core.location.Place
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.weatherCache by preferencesDataStore(name = "weather_cache")

/** Caches forecasts per location and only calls the provider once the cached data expires. */
class WeatherRepository(context: Context, private val provider: WeatherProvider) {

    private val dataStore = context.applicationContext.weatherCache
    private val json = Json { ignoreUnknownKeys = true }

    val attribution: String get() = provider.attribution

    /** Emits whenever any cached forecast changes, so open widgets can redraw. */
    val changes: Flow<Any> get() = dataStore.data

    suspend fun cached(place: Place): WeatherData? {
        val raw = dataStore.data.first()[key(place)] ?: return null
        return runCatching { json.decodeFromString<WeatherData>(raw) }.getOrNull()
    }

    /** True when the forecast has expired and we haven't just tried (and possibly failed) to fetch it. */
    suspend fun needsRefresh(place: Place): Boolean {
        val prefs = dataStore.data.first()
        val now = System.currentTimeMillis()
        val expired = cached(place)?.let { now >= it.expiresAt } ?: true
        val recentlyTried = now - (prefs[attemptKey(place)] ?: 0L) < RETRY_INTERVAL_MILLIS
        return expired && !recentlyTried
    }

    /** Returns fresh data when possible, otherwise whatever is cached (possibly null). */
    suspend fun refresh(place: Place, force: Boolean = false): WeatherData? {
        if (!force && !needsRefresh(place)) return cached(place)
        dataStore.edit { it[attemptKey(place)] = System.currentTimeMillis() }
        return try {
            provider.fetch(place.latitude, place.longitude).also { fresh ->
                dataStore.edit { it[key(place)] = json.encodeToString(WeatherData.serializer(), fresh) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Weather refresh failed for ${place.cacheKey}", e)
            cached(place)
        }
    }

    private fun key(place: Place) = stringPreferencesKey(place.cacheKey)

    private fun attemptKey(place: Place) = longPreferencesKey("attempt:${place.cacheKey}")

    private companion object {
        const val TAG = "WeatherRepository"

        /** Keeps a failing API (or no network) from causing a refresh loop. */
        const val RETRY_INTERVAL_MILLIS = 15 * 60 * 1000L
    }
}
