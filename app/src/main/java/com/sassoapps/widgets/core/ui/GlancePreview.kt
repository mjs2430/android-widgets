package com.sassoapps.widgets.core.ui

import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.GlanceComposable
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews

/**
 * Renders real widget (Glance) content inside the app, so previews match the home screen
 * exactly without duplicating any UI code. Re-renders whenever [key] or [size] changes.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
fun GlancePreview(key: Any, size: DpSize, modifier: Modifier = Modifier, content: @GlanceComposable @Composable () -> Unit) {
    val context = LocalContext.current
    val latestContent by rememberUpdatedState(content)
    var views by remember { mutableStateOf<RemoteViews?>(null) }

    LaunchedEffect(key, size) {
        views = GlanceRemoteViews().compose(context, size) { latestContent() }.remoteViews
    }

    Box(modifier.size(size)) {
        AndroidView(
            factory = { FrameLayout(it) },
            update = { frame ->
                frame.removeAllViews()
                views?.let { frame.addView(it.apply(context, frame)) }
            },
            modifier = Modifier.matchParentSize(),
        )
        // Swallow taps so the preview doesn't launch the widget's actions.
        Box(Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {})
    }
}
