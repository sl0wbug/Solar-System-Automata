package com.droidlinkstd.solarsystemautomata.ui.inspector

import com.droidlinkstd.solarsystemautomata.data.CelestialBody
import com.droidlinkstd.solarsystemautomata.domain.physics.PhysicsState
import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import com.droidlinkstd.solarsystemautomata.domain.physics.SimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Unit test suite for telemetry extraction and body deletion mechanics.
 */
class BodyInspectorTest {

    @Test
    fun testRenderSnapshot_copiesVelocitiesAndMass() {
        val state = PhysicsState(capacity = 4)
        val body = CelestialBody(
            id = 1,
            name = "TestBody",
            mass = 42.5,
            positionX = 3.0,
            positionY = 4.0,
            velocityX = -1.5,
            velocityY = 2.5,
            radius = 5.0,
            colorHex = 0xFF38BDF8L
        )
        state.loadFromDomain(listOf(body))

        val snapshot = RenderSnapshot(capacity = 4)
        snapshot.copyFrom(state)

        assertEquals(1, snapshot.count)
        assertEquals("TestBody", snapshot.names[0])
        assertEquals(3.0, snapshot.posX[0], 1e-9)
        assertEquals(4.0, snapshot.posY[0], 1e-9)
        assertEquals(-1.5, snapshot.velX[0], 1e-9)
        assertEquals(2.5, snapshot.velY[0], 1e-9)
        assertEquals(42.5, snapshot.mass[0], 1e-9)
        assertEquals(5.0f, snapshot.radius[0], 1e-6f)
    }

    @Test
    fun testSimulationEngine_deleteBodyAt_performsSwapAndPop() {
        val engine = SimulationEngine(capacity = 8)
        val bodies = listOf(
            CelestialBody(id = 1, name = "Alpha", mass = 10.0, positionX = 1.0, positionY = 0.0, velocityX = 0.0, velocityY = 1.0, radius = 2.0, colorHex = 0xFF111111L),
            CelestialBody(id = 2, name = "Beta", mass = 20.0, positionX = 2.0, positionY = 0.0, velocityX = 0.0, velocityY = 2.0, radius = 3.0, colorHex = 0xFF222222L),
            CelestialBody(id = 3, name = "Gamma", mass = 30.0, positionX = 3.0, positionY = 0.0, velocityX = 0.0, velocityY = 3.0, radius = 4.0, colorHex = 0xFF333333L)
        )
        engine.loadBodies(bodies)
        assertEquals(3, engine.getRenderSnapshot().count)

        // Delete first body (Alpha at index 0)
        val success = engine.deleteBodyAt(0)
        assertTrue(success)

        val snapshot = engine.getRenderSnapshot()
        assertEquals(2, snapshot.count)

        // Last body (Gamma) must be swapped into index 0
        assertEquals("Gamma", snapshot.names[0])
        assertEquals(30.0, snapshot.mass[0], 1e-9)
        assertEquals(3.0, snapshot.posX[0], 1e-9)
        assertEquals(3.0, snapshot.velY[0], 1e-9)

        // Beta remains at index 1
        assertEquals("Beta", snapshot.names[1])
        assertEquals(20.0, snapshot.mass[1], 1e-9)

        // Out of bounds deletion returns false
        assertFalse(engine.deleteBodyAt(99))
        assertFalse(engine.deleteBodyAt(-1))
    }

    @Test
    fun testTelemetryCalculations_speedAndDistance() {
        val vx = 3.0
        val vy = 4.0
        val speed = hypot(vx, vy)
        assertEquals(5.0, speed, 1e-9)

        val x = -6.0
        val y = 8.0
        val distance = hypot(x, y)
        assertEquals(10.0, distance, 1e-9)
    }
}
