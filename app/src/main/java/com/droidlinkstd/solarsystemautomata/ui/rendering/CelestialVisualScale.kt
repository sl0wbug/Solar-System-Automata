package com.droidlinkstd.solarsystemautomata.ui.rendering

/**
 * Mathematical continuous visual scale mapping astronomical physical models
 * to screen pixel radii.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Mathematical hierarchy: Replaces hard fixed clamps with continuous power-law scaling.
 * - Natural proportions:
 *   - Central Sun (r = 109.0): ~42.8px base radius (with multi-layer radiant corona ~94px)
 *   - Gas Giants (r ≈ 9.5 - 11.2): ~10.6 - 11.6px base radius
 *   - Ice Giants (r ≈ 3.9 - 4.0): ~6.6 - 6.7px base radius
 *   - Terrestrials (r ≈ 0.38 - 1.0): ~2.4 - 3.5px base radius
 *   - Natural Satellites / Moons (r ≈ 0.01 - 0.41): ~1.2 - 2.5px base radius
 * - Zero allocations during runtime calculations.
 * - Smooth continuous scaling with pinch-to-zoom gestures.
 */
object CelestialVisualScale {

    const val REFERENCE_ZOOM: Float = 15.0f

    /**
     * Calculates mathematical perceptual radius in screen pixels.
     * Guaranteed zero heap allocations.
     *
     * @param radiusModel Physical radius in Earth radii ($R_\oplus$).
     * @param zoom Current camera viewport zoom in pixels/AU.
     */
    fun calculateVisualRadiusPx(radiusModel: Float, zoom: Float): Float {
        if (radiusModel <= 0f) return 1.2f

        val zoomRatio = (zoom / REFERENCE_ZOOM).coerceIn(0.01f, 1000f).toDouble()
        val zoomFactor = Math.pow(zoomRatio, 0.28)

        val base = 2.5 * Math.pow(radiusModel.toDouble(), 0.60) + 1.0
        return (base * zoomFactor).toFloat()
    }
}
