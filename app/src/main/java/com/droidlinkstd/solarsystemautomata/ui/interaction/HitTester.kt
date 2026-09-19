package com.droidlinkstd.solarsystemautomata.ui.interaction

import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import kotlin.math.max

/**
 * Screen-space hit tester for selecting celestial bodies via touch.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Zero Heap Allocations: Uses flat primitive scalar arithmetic.
 * - Minimum Hit Radius: Ensures small or distant astronomical bodies remain easily selectable
 *   by evaluating visual screen radius against [touchSlopPx].
 * - Closest-Target Resolution: When bodies overlap or touch slops intersect, selects
 *   the body with the smallest Euclidean distance to the tap point.
 */
object HitTester {

    const val DEFAULT_TOUCH_SLOP_PX: Float = 36.0f

    /**
     * Finds the index of the closest celestial body within touch range of ([screenX], [screenY]).
     * Returns -1 if no body was hit.
     * Guaranteed zero heap allocations.
     */
    fun findBodyAtScreenOffset(
        screenX: Float,
        screenY: Float,
        snapshot: RenderSnapshot,
        cameraState: CameraState,
        touchSlopPx: Float = DEFAULT_TOUCH_SLOP_PX
    ): Int {
        val count = snapshot.count
        if (count <= 0) return -1

        var closestIndex = -1
        var minDistanceSq = Float.MAX_VALUE

        var i = 0
        while (i < count) {
            val wx = snapshot.posX[i]
            val wy = snapshot.posY[i]
            val sx = cameraState.worldToScreenX(wx)
            val sy = cameraState.worldToScreenY(wy)

            val visualRadiusPx = com.droidlinkstd.solarsystemautomata.ui.rendering.CelestialVisualScale.calculateVisualRadiusPx(
                snapshot.radius[i],
                cameraState.zoom
            )
            val effectiveHitRadius = max(visualRadiusPx, touchSlopPx)
            val effectiveHitRadiusSq = effectiveHitRadius * effectiveHitRadius

            val dx = screenX - sx
            val dy = screenY - sy
            val distSq = dx * dx + dy * dy

            if (distSq <= effectiveHitRadiusSq && distSq < minDistanceSq) {
                minDistanceSq = distSq
                closestIndex = i
            }
            i++
        }

        return closestIndex
    }
}
