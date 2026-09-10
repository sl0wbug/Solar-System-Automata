package com.droidlinkstd.solarsystemautomata.ui.rendering

import android.graphics.Canvas
import android.graphics.Paint
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import kotlin.math.max

/**
 * Pre-allocated circular ring buffer managing active celestial impact shockwave rings.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Zero Heap Allocations: Flat primitive arrays track active shockwave state.
 * - Thread-Safe Emission: [triggerShockwave] synchronizes to permit injection from the physics thread.
 * - Hardware-Accelerated Draw Phase: Renders expanding, alpha-fading ripples on the native canvas.
 */
class ShockwaveBuffer(val capacity: Int = DEFAULT_CAPACITY) {

    private val worldX = DoubleArray(capacity)
    private val worldY = DoubleArray(capacity)
    private val currentRadiusPx = FloatArray(capacity)
    private val maxRadiusPx = FloatArray(capacity)
    private val alpha = FloatArray(capacity)
    private val color = IntArray(capacity)
    private val isActive = BooleanArray(capacity)

    private var writeHead = 0

    /**
     * Triggers a new expanding impact ripple at world coordinates ([x], [y]).
     * Thread-safe for invocation across background physics and UI rendering threads.
     * Zero heap allocation.
     */
    @Synchronized
    fun triggerShockwave(
        x: Double,
        y: Double,
        impactRadiusPx: Float,
        impactColor: Int
    ) {
        val idx = writeHead
        worldX[idx] = x
        worldY[idx] = y
        currentRadiusPx[idx] = 6.0f
        maxRadiusPx[idx] = max(impactRadiusPx * 2.2f, 40.0f)
        alpha[idx] = 1.0f
        color[idx] = impactColor
        isActive[idx] = true

        writeHead = (writeHead + 1) % capacity
    }

    /**
     * Advances shockwave radius expansion and alpha dissipation.
     * Guaranteed zero heap allocations.
     */
    fun update(dtSec: Float) {
        if (dtSec <= 0f) return

        val expansionRate = 90.0f // px/sec expansion
        val fadeRate = 1.8f       // alpha decay rate (~0.55s total duration)

        var i = 0
        while (i < capacity) {
            if (isActive[i]) {
                currentRadiusPx[i] += expansionRate * dtSec
                alpha[i] -= fadeRate * dtSec

                if (alpha[i] <= 0.0f || currentRadiusPx[i] >= maxRadiusPx[i]) {
                    isActive[i] = false
                    alpha[i] = 0.0f
                }
            }
            i++
        }
    }

    /**
     * Renders all active shockwaves onto [nativeCanvas].
     * Zero heap allocations.
     */
    fun draw(
        nativeCanvas: Canvas,
        cameraState: CameraState,
        shockwavePaint: Paint,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        var i = 0
        while (i < capacity) {
            if (isActive[i]) {
                val sx = cameraState.worldToScreenX(worldX[i])
                val sy = cameraState.worldToScreenY(worldY[i])
                val rad = currentRadiusPx[i]

                // Viewport culling
                if (sx + rad >= 0f && sx - rad <= canvasWidth &&
                    sy + rad >= 0f && sy - rad <= canvasHeight
                ) {
                    val c = color[i]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val a = (alpha[i] * 255.0f).toInt().coerceIn(0, 255)

                    shockwavePaint.setARGB(a, r, g, b)
                    shockwavePaint.strokeWidth = (3.5f * alpha[i]).coerceAtLeast(1.0f)

                    nativeCanvas.drawCircle(sx, sy, rad, shockwavePaint)
                }
            }
            i++
        }
    }

    /**
     * Clears all active shockwaves.
     */
    @Synchronized
    fun clear() {
        var i = 0
        while (i < capacity) {
            isActive[i] = false
            alpha[i] = 0f
            currentRadiusPx[i] = 0f
            i++
        }
        writeHead = 0
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 16
    }
}
