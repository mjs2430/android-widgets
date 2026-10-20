package com.sassoapps.widgets.core.style

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap

/**
 * Draws gradient and "glass" widget backgrounds into a bitmap.
 *
 * Home screen widgets can't blur the wallpaper behind them, so glass is simulated with a
 * translucent tint, a light sheen across the top and a bright edge highlight.
 */
object BackgroundRenderer {

    /** Themes that need a bitmap. The rest use a plain color. */
    fun needsBitmap(theme: ThemePreset): Boolean = when (theme) {
        ThemePreset.LIQUID_GLASS, ThemePreset.SOFT_GLASS, ThemePreset.GRADIENT -> true
        ThemePreset.TRANSPARENT, ThemePreset.SOLID, ThemePreset.MATERIAL_YOU -> false
    }

    /**
     * @param scale pixels per dp to render at. Kept modest so widget bitmaps stay small.
     */
    fun render(style: WidgetStyle, widthDp: Float, heightDp: Float, scale: Float): Bitmap {
        val width = (widthDp * scale).toInt().coerceAtLeast(1)
        val height = (heightDp * scale).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val radius = style.cornerRadiusDp * scale

        when (style.theme) {
            ThemePreset.LIQUID_GLASS -> drawLiquidGlass(canvas, rect, radius, scale, style)
            ThemePreset.SOFT_GLASS -> drawSoftGlass(canvas, rect, radius, scale, style)
            ThemePreset.GRADIENT -> drawGradient(canvas, rect, radius, style)
            else -> Unit
        }
        return bitmap
    }

    private fun drawLiquidGlass(canvas: Canvas, rect: RectF, radius: Float, scale: Float, style: WidgetStyle) {
        fill(canvas, rect, radius, style.backgroundColor.withAlpha(style.opacity))

        // Sheen: light falling off from the top edge.
        fill(canvas, rect, radius, shader = LinearGradient(
            0f, 0f, 0f, rect.height() * 0.6f,
            WHITE.withAlpha(0.22f), WHITE.withAlpha(0f), Shader.TileMode.CLAMP,
        ))
        // Specular glow near the top-left corner.
        fill(canvas, rect, radius, shader = RadialGradient(
            rect.width() * 0.18f, -rect.height() * 0.15f, rect.width() * 0.55f,
            WHITE.withAlpha(0.20f), WHITE.withAlpha(0f), Shader.TileMode.CLAMP,
        ))
        // Faint light caught along the bottom, like refraction through thick glass.
        fill(canvas, rect, radius, shader = LinearGradient(
            0f, rect.height() * 0.7f, 0f, rect.height(),
            WHITE.withAlpha(0f), WHITE.withAlpha(0.08f), Shader.TileMode.CLAMP,
        ))
        // Edge highlight: bright top-left, dim middle, soft bottom-right.
        stroke(canvas, rect, radius, 1.2f * scale, LinearGradient(
            0f, 0f, rect.width(), rect.height(),
            intArrayOf(WHITE.withAlpha(0.70f), WHITE.withAlpha(0.08f), WHITE.withAlpha(0.35f)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        ))
    }

    private fun drawSoftGlass(canvas: Canvas, rect: RectF, radius: Float, scale: Float, style: WidgetStyle) {
        fill(canvas, rect, radius, style.backgroundColor.withAlpha(style.opacity))
        fill(canvas, rect, radius, shader = LinearGradient(
            0f, 0f, 0f, rect.height(),
            WHITE.withAlpha(0.10f), WHITE.withAlpha(0.02f), Shader.TileMode.CLAMP,
        ))
        stroke(canvas, rect, radius, 1f * scale, LinearGradient(
            0f, 0f, 0f, rect.height(),
            WHITE.withAlpha(0.22f), WHITE.withAlpha(0.10f), Shader.TileMode.CLAMP,
        ))
    }

    private fun drawGradient(canvas: Canvas, rect: RectF, radius: Float, style: WidgetStyle) {
        fill(canvas, rect, radius, shader = LinearGradient(
            0f, 0f, rect.width(), rect.height(),
            style.backgroundColor.withAlpha(style.opacity),
            style.gradientEndColor.withAlpha(style.opacity),
            Shader.TileMode.CLAMP,
        ))
    }

    private fun fill(canvas: Canvas, rect: RectF, radius: Float, color: Int = 0, shader: Shader? = null) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // A shader is still multiplied by the paint's alpha, so keep it opaque when shading.
            this.color = if (shader != null) WHITE else color
            this.shader = shader
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun stroke(canvas: Canvas, rect: RectF, radius: Float, width: Float, shader: Shader) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            this.shader = shader
        }
        val inset = width / 2
        val edge = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        canvas.drawRoundRect(edge, radius - inset, radius - inset, paint)
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
}
