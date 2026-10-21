package com.sassoapps.widgets.widgets.today.config

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sassoapps.widgets.core.ui.WidgetsTheme
import kotlinx.coroutines.launch

/**
 * Settings for one Today widget. Opened by the launcher (long-press → reconfigure),
 * from the app's home screen, or by tapping the widget's weather section.
 */
class TodayConfigActivity : ComponentActivity() {

    private val appWidgetId by lazy {
        intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private val viewModel: TodayConfigViewModel by viewModels {
        viewModelFactory { initializer { TodayConfigViewModel(application as Application, appWidgetId) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Defaults are fine, so placing the widget succeeds even if the user backs out right away.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        enableEdgeToEdge()
        setContent {
            WidgetsTheme {
                TodayConfigScreen(viewModel, onDone = ::saveAndFinish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDeviceLocation()
    }

    private fun saveAndFinish() {
        lifecycleScope.launch {
            viewModel.save()
            finish()
        }
    }
}
