package com.sassoapps.widgets.core.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val Context.widgetConfigs by preferencesDataStore(name = "widget_configs")

/** Lenient so configs saved by older app versions still load after fields are added or removed. */
internal val ConfigJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

/**
 * Stores one config object per placed widget, keyed by its appWidgetId.
 * Each widget type gets its own [widgetType] prefix so future widgets can share the same store.
 */
class WidgetConfigStore<T>(
    context: Context,
    private val widgetType: String,
    private val serializer: KSerializer<T>,
    private val default: () -> T,
) {
    private val dataStore = context.applicationContext.widgetConfigs

    fun flow(appWidgetId: Int): Flow<T> =
        dataStore.data.map { prefs -> decode(prefs[key(appWidgetId)]) }

    suspend fun get(appWidgetId: Int): T = flow(appWidgetId).first()

    suspend fun set(appWidgetId: Int, config: T) {
        dataStore.edit { it[key(appWidgetId)] = ConfigJson.encodeToString(serializer, config) }
    }

    suspend fun delete(appWidgetId: Int) {
        dataStore.edit { it.remove(key(appWidgetId)) }
    }

    private fun key(appWidgetId: Int) = stringPreferencesKey("$widgetType:$appWidgetId")

    private fun decode(json: String?): T =
        json?.let { runCatching { ConfigJson.decodeFromString(serializer, it) }.getOrNull() } ?: default()
}
