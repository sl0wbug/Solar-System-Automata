package com.droidlinkstd.solarsystemautomata.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CelestialVisualScaleTest {

    @Test
    fun referenceZoomYieldsUnitZoomFactor() {
        val zoomFactor = CelestialVisualScale.calculateZoomFactor(CelestialVisualScale.REFERENCE_ZOOM)
        assertEquals(1.0, zoomFactor, 1e-6)
    }

    @Test
    fun precomputedZoomFactorMatchesDirectZoomOverload() {
        val zoom = 45.0f
        val zoomFactor = CelestialVisualScale.calculateZoomFactor(zoom)

        val testRadii = floatArrayOf(0.1f, 1.0f, 4.0f, 11.2f, 109.0f)
        for (radius in testRadii) {
            val direct = CelestialVisualScale.calculateVisualRadiusPx(radius, zoom)
            val optimized = CelestialVisualScale.calculateVisualRadiusPx(radius, zoomFactor)
            assertEquals("Optimized zoomFactor must match direct calculation for radius $radius", direct, optimized, 1e-6f)
        }
    }

    @Test
    fun invalidOrZeroRadiusReturnsMinimumThreshold() {
        val zoomFactor = CelestialVisualScale.calculateZoomFactor(15.0f)
        assertEquals(1.2f, CelestialVisualScale.calculateVisualRadiusPx(0f, zoomFactor), 1e-6f)
        assertEquals(1.2f, CelestialVisualScale.calculateVisualRadiusPx(-5f, zoomFactor), 1e-6f)
    }

    @Test
    fun visualHierarchyMaintainedAcrossScales() {
        val zoomFactor = CelestialVisualScale.calculateZoomFactor(15.0f)

        val moonRadius = CelestialVisualScale.calculateVisualRadiusPx(0.27f, zoomFactor)
        val earthRadius = CelestialVisualScale.calculateVisualRadiusPx(1.0f, zoomFactor)
        val jupiterRadius = CelestialVisualScale.calculateVisualRadiusPx(11.2f, zoomFactor)
        val sunRadius = CelestialVisualScale.calculateVisualRadiusPx(109.0f, zoomFactor)

        assertTrue(moonRadius < earthRadius)
        assertTrue(earthRadius < jupiterRadius)
        assertTrue(jupiterRadius < sunRadius)
    }
}
