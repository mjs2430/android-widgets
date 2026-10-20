package com.sassoapps.widgets.core.apps

import android.content.Context
import android.content.Intent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What happens when the user taps part of a widget. */
@Serializable
sealed interface LaunchTarget {
    /** The section's built-in behavior, e.g. the default clock app for the clock. */
    @Serializable
    @SerialName("default")
    data object Default : LaunchTarget

    @Serializable
    @SerialName("app")
    data class App(val packageName: String, val label: String) : LaunchTarget

    @Serializable
    @SerialName("none")
    data object None : LaunchTarget
}

/**
 * Resolves the target to an intent, using [default] for [LaunchTarget.Default].
 * Returns null when tapping should do nothing (or the chosen app was uninstalled).
 */
fun LaunchTarget.toIntent(context: Context, default: () -> Intent?): Intent? = when (this) {
    LaunchTarget.Default -> default()
    is LaunchTarget.App -> context.packageManager.getLaunchIntentForPackage(packageName)
    LaunchTarget.None -> null
}
