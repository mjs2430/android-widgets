package com.sassoapps.widgets.widgets.today.config

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sassoapps.widgets.appContainer
import com.sassoapps.widgets.core.location.Place
import com.sassoapps.widgets.core.location.WeatherLocation
import com.sassoapps.widgets.core.ui.ChoiceRow
import com.sassoapps.widgets.core.ui.LaunchTargetRow
import com.sassoapps.widgets.core.ui.SettingsGroup
import com.sassoapps.widgets.core.ui.SliderRow
import com.sassoapps.widgets.core.ui.SwitchRow
import com.sassoapps.widgets.core.ui.rememberPermissionState
import com.sassoapps.widgets.core.weather.TemperatureUnit
import com.sassoapps.widgets.widgets.today.ForecastKind
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.WeatherConfig
import kotlinx.coroutines.launch

@Composable
internal fun WeatherTab(
    config: TodayConfig,
    place: Place?,
    viewModel: TodayConfigViewModel,
    update: ((TodayConfig) -> TodayConfig) -> Unit,
) {
    val weather = config.weather
    fun edit(transform: (WeatherConfig) -> WeatherConfig) = update { it.copy(weather = transform(it.weather)) }

    SettingsGroup("Location") {
        val useDevice = weather.location == WeatherLocation.Device
        ChoiceRow("Weather for", listOf(true, false), useDevice, { if (it) "Current location" else "A city" }) { device ->
            if (device) edit { it.copy(location = WeatherLocation.Device) }
        }
        if (useDevice) {
            DeviceLocation(place, onGranted = viewModel::refreshDeviceLocation)
        } else {
            Text("Showing: ${place?.name ?: "none yet"}", style = MaterialTheme.typography.bodyMedium)
        }
        PlaceSearch(viewModel) { picked -> edit { it.copy(location = WeatherLocation.Fixed(picked)) } }
    }
    SettingsGroup("Show") {
        SwitchRow("Place name", weather.showPlaceName, { v -> edit { it.copy(showPlaceName = v) } }, subtitle = "e.g. Raleigh, NC")
        SwitchRow("Condition", weather.showCondition, { v -> edit { it.copy(showCondition = v) } }, subtitle = "e.g. Partly cloudy")
        SwitchRow("Today's high and low", weather.showHighLow, { v -> edit { it.copy(showHighLow = v) } })
    }
    SettingsGroup("Forecast") {
        ChoiceRow("Show first", ForecastKind.entries, weather.forecast, { it.label }) { k -> edit { it.copy(forecast = k) } }
        SliderRow("Items", weather.forecastCount, 0..6, { v -> edit { it.copy(forecastCount = v) } }) {
            if (it == 0) "off" else "$it"
        }
        Text(
            "Tap the forecast on your home screen to switch between hourly and daily. Fewer items show when the widget is narrow, and the compact layout has none.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChoiceRow("Units", TemperatureUnit.entries, weather.unit, { it.label }) { u -> edit { it.copy(unit = u) } }
        SliderRow("Text size", weather.textSizeSp, 10..28, { v -> edit { it.copy(textSizeSp = v) } })
    }
    SettingsGroup("Tap action") {
        LaunchTargetRow("Tapping the weather opens", weather.tapTarget, "These widget settings") { t -> edit { it.copy(tapTarget = t) } }
    }
    val context = LocalContext.current
    Text(
        context.appContainer.weather.attribution,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun DeviceLocation(place: Place?, onGranted: () -> Unit) {
    val permission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION) { if (it) onGranted() }
    if (permission.granted) {
        Text("Showing: ${place?.name ?: "finding your location…"}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Updated whenever you open this app. Only your approximate location is used.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text("Allow approximate location, or search for a city below.")
        Button(onClick = permission.request, modifier = Modifier.padding(vertical = 8.dp)) { Text("Allow location") }
    }
}

@Composable
private fun PlaceSearch(viewModel: TodayConfigViewModel, onPick: (Place) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    fun search() = scope.launch { results = viewModel.searchPlaces(query) }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search for a city") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    results?.let { found ->
        if (found.isEmpty()) Text("No matches", Modifier.padding(8.dp))
        found.forEach { place ->
            Text(
                place.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().clickable {
                    onPick(place)
                    results = null
                    query = ""
                }.padding(vertical = 10.dp),
            )
        }
    }
}
