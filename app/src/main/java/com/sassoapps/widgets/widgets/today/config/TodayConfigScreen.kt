package com.sassoapps.widgets.widgets.today.config

import android.appwidget.AppWidgetManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sassoapps.widgets.core.ui.GlancePreview
import com.sassoapps.widgets.widgets.today.TodayConfig
import com.sassoapps.widgets.widgets.today.TodayContent
import com.sassoapps.widgets.widgets.today.TodayData

private enum class ConfigTab(val label: String) { LAYOUT("Layout"), CLOCK("Clock"), CALENDAR("Calendar"), WEATHER("Weather"), STYLE("Style") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayConfigScreen(viewModel: TodayConfigViewModel, onDone: () -> Unit) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(onBack = onDone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today widget") },
                actions = { TextButton(onClick = onDone) { Text("Done") } },
            )
        },
    ) { padding ->
        val current = config
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            LivePreview(current, data ?: TodayData(System.currentTimeMillis(), true, null, null, null), viewModel.appWidgetId)
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                ConfigTab.entries.forEachIndexed { index, it ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(it.label) })
                }
            }
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val update = viewModel::update
                when (ConfigTab.entries[tab]) {
                    ConfigTab.LAYOUT -> LayoutTab(current, update)
                    ConfigTab.CLOCK -> ClockTab(current, update)
                    ConfigTab.CALENDAR -> CalendarTab(current, viewModel, update)
                    ConfigTab.WEATHER -> WeatherTab(current, data?.place, viewModel, update)
                    ConfigTab.STYLE -> StyleTab(current, update)
                }
            }
        }
    }
}

/** The real widget, rendered over a colorful backdrop so glass and transparency are visible. */
@Composable
private fun LivePreview(config: TodayConfig, data: TodayData, appWidgetId: Int) {
    val size = rememberWidgetSize(appWidgetId)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF3A6073), Color(0xFF16222A), Color(0xFF8E54E9))))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fitted = DpSize(minOf(size.width, maxWidth), size.height)
        GlancePreview(key = config to data, size = fitted) {
            TodayContent(config, data, appWidgetId)
        }
    }
}

/** The widget's actual size on the home screen (portrait), or a typical 4×2 size. */
@Composable
private fun rememberWidgetSize(appWidgetId: Int): DpSize {
    val context = LocalContext.current
    return remember(appWidgetId) {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (width > 0 && height > 0) DpSize(width.dp, height.dp) else DpSize(340.dp, 170.dp)
    }
}
