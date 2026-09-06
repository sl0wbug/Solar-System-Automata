package com.droidlinkstd.solarsystemautomata.ui.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import java.util.Random

/**
 * Pre-allocated starfield background buffer providing depth and parallax cues.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Deterministic static initialization via seeded [Random].
 * - Zero allocations during draw frames: stars are stored in flat primitive arrays
 *   and drawn directly to native canvas with pre-allocated [Paint].
 */
class StarfieldBuffer(val starCount: Int = DEFAULT_STAR_COUNT) {
    companion object {
        const val DEFAULT_STAR_COUNT: Int = 180
    }

    private val normX = FloatArray(starCount)
    private val normY = FloatArray(starCount)
    private val radius = FloatArray(starCount)
    private val alphaInt = IntArray(starCount) // 0..255 for Paint.alpha
    private val parallax = FloatArray(starCount)

    private val starPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    init {
        val rng = Random(1337L)
        var i = 0
        while (i < starCount) {
            normX[i] = rng.nextFloat()
            normY[i] = rng.nextFloat()
            radius[i] = 0.75f + rng.nextFloat() * 1.5f
            alphaInt[i] = (50 + rng.nextInt(180)).coerceIn(0, 255)
            parallax[i] = 0.02f + rng.nextFloat() * 0.06f
            i++
        }
    }

    /**
     * Draws all stars onto the [nativeCanvas] with subtle camera parallax.
     * Zero heap allocation.
     */
    fun draw(nativeCanvas: Canvas, width: Float, height: Float, camera: CameraState) {
        if (width <= 0f || height <= 0f) return

        // Subtle pan parallax based on camera center (scaled to prevent overflow)
        val panShiftX = (camera.centerX * 1e-10).toFloat()
        val panShiftY = (camera.centerY * 1e-10).toFloat()

        var i = 0
        while (i < starCount) {
            val shiftX = panShiftX * parallax[i]
            val shiftY = panShiftY * parallax[i]

            var px = (normX[i] * width + shiftX) % width
            if (px < 0f) px += width

            var py = (normY[i] * height + shiftY) % height
            if (py < 0f) py += height

            starPaint.alpha = alphaInt[i]
            nativeCanvas.drawCircle(px, py, radius[i], starPaint)
            i++
        }
    }
}
