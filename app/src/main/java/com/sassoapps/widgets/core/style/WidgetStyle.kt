package com.sassoapps.widgets.core.style

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePreset(val label: String) {
    LIQUID_GLASS("Liquid glass"),
    SOFT_GLASS("Soft glass"),
    TRANSPARENT("Transparent"),
    SOLID("Solid"),
    GRADIENT("Gradient"),
    MATERIAL_YOU("Material You"),
}

/** System font families. Widgets can only use fonts built into the device. */
@Serializable
enum class FontChoice(val label: String, val family: String) {
    SANS("Sans", "sans-serif"),
    SANS_LIGHT("Light", "sans-serif-light"),
    SANS_THIN("Thin", "sans-serif-thin"),
    SANS_MEDIUM("Medium", "sans-serif-medium"),
    CONDENSED("Condensed", "sans-serif-condensed"),
    SERIF("Serif", "serif"),
    MONO("Mono", "monospace"),
}

/** Visual style shared by every widget. Colors are ARGB ints. */
@Serializable
data class WidgetStyle(
    val theme: ThemePreset = ThemePreset.SOFT_GLASS,
    /** Background strength, 0 (invisible) to 1 (fully opaque). Ignored by [ThemePreset.TRANSPARENT]. */
    val opacity: Float = 0.30f,
    val cornerRadiusDp: Int = 24,
    val paddingDp: Int = 16,
    /** Solid color, glass tint, or gradient start. */
    val backgroundColor: Int = 0xFF000000.toInt(),
    val gradientEndColor: Int = 0xFF3D5AFE.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val fontFamily: FontChoice = FontChoice.SANS,
)

/** Applies [alpha] (0..1) to an ARGB color, replacing its existing alpha. */
fun Int.withAlpha(alpha: Float): Int =
    ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (this and 0x00FFFFFF)
