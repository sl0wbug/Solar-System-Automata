package com.droidlinkstd.solarsystemautomata.domain.physics

import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import com.droidlinkstd.solarsystemautomata.ui.rendering.OrbitalTrailBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cbrt

/**
 * Comprehensive verification suite for Milestone 3:
 * Inelastic Collisions, Swap-and-Pop Compaction, and VFX synchronization.
 */
class CollisionPhysicsTest {

    private lateinit var state: PhysicsState
    private lateinit var integrator: OrbitalIntegrator

    @Before
    fun setUp() {
        state = PhysicsState(capacity = 16)
        integrator = OrbitalIntegrator()
    }

    @Test
    fun inelasticCollisionConservesTotalLinearMomentum() {
        // Body 0 (Earth-like): Mass 1.0, Velocity (0.5, -0.2)
        state.posX[0] = 0.0
        state.posY[0] = 0.0
        state.velX[0] = 0.5
        state.velY[0] = -0.2
        state.mass[0] = 1.0
        state.radius[0] = 8f
        state.names[0] = "PlanetA"

        // Body 1 (Asteroid): Mass 0.05, Velocity (-1.2, 0.8)
        // Position within collision threshold ((8 + 2) * 4.25875e-5 = 4.25875e-4 AU)
        state.posX[1] = 0.0001
        state.posY[1] = 0.0001
        state.velX[1] = -1.2
        state.velY[1] = 0.8
        state.mass[1] = 0.05
        state.radius[1] = 2f
        state.names[1] = "AsteroidB"

        state.count = 2

        val initialPx = state.calculateTotalLinearMomentumX()
        val initialPy = state.calculateTotalLinearMomentumY()

        val collided = integrator.resolveCollisions(state)

        assertTrue("Collision should have occurred between overlapping bodies", collided)
        assertEquals("Body count should decrease by 1 after merger", 1, state.count)

        val finalPx = state.calculateTotalLinearMomentumX()
        val finalPy = state.calculateTotalLinearMomentumY()

        assertEquals("Total Px must be conserved within 1e-9 tolerance", initialPx, finalPx, 1e-9)
        assertEquals("Total Py must be conserved within 1e-9 tolerance", initialPy, finalPy, 1e-9)
    }

    @Test
    fun inelasticCollisionConservesTotalMass() {
        val m1 = 15.5
        val m2 = 4.75

        state.posX[0] = 0.0
        state.posY[0] = 0.0
        state.mass[0] = m1
        state.radius[0] = 10f
        state.names[0] = "Heavy"

        state.posX[1] = 0.00005
        state.posY[1] = 0.00005
        state.mass[1] = m2
        state.radius[1] = 5f
        state.names[1] = "Light"

        state.count = 2

        val initialTotalMass = m1 + m2
        integrator.resolveCollisions(state)

        assertEquals(1, state.count)
        assertEquals("Total mass must be preserved exactly", initialTotalMass, state.mass[0], 1e-9)
        assertEquals("Heavier body should retain its name", "Heavy", state.names[0])
    }

    @Test
    fun inelasticCollisionConservesVolume() {
        val r1 = 8.0f
        val r2 = 6.0f

        state.posX[0] = 0.0
        state.posY[0] = 0.0
        state.mass[0] = 2.0
        state.radius[0] = r1

        state.posX[1] = 0.00001
        state.posY[1] = 0.0
        state.mass[1] = 1.0
        state.radius[1] = r2

        state.count = 2

        val expectedRadius = cbrt(r1 * r1 * r1 + r2 * r2 * r2)
        integrator.resolveCollisions(state)

        assertEquals("Merged body radius must reflect volume conservation", expectedRadius, state.radius[0], 0.001f)
    }

    @Test
    fun barycentricPositionIsCenterOfMass() {
        val m1 = 3.0
        val m2 = 1.0
        val x1 = 0.0
        val y1 = 0.0
        val x2 = 0.00008
        val y2 = 0.00004

        state.posX[0] = x1
        state.posY[0] = y1
        state.mass[0] = m1
        state.radius[0] = 6f

        state.posX[1] = x2
        state.posY[1] = y2
        state.mass[1] = m2
        state.radius[1] = 4f

        state.count = 2

        val expectedX = (m1 * x1 + m2 * x2) / (m1 + m2)
        val expectedY = (m1 * y1 + m2 * y2) / (m1 + m2)

        integrator.resolveCollisions(state)

        assertEquals("Barycenter X must match center of mass", expectedX, state.posX[0], 1e-9)
        assertEquals("Barycenter Y must match center of mass", expectedY, state.posY[0], 1e-9)
    }

    @Test
    fun swapAndPopMaintainsArrayIntegrityAndZeroesVacatedSlot() {
        // Setup 4 bodies: [A, B, C, D]
        // B (idx 1, mass 10.0) collides with and absorbs C (idx 2, mass 1.0)
        // Expected result: [A, B_merged, D], count = 3, slot 3 zeroed out.
        state.names[0] = "A"; state.mass[0] = 5.0; state.posX[0] = -10.0; state.radius[0] = 5f
        state.names[1] = "B"; state.mass[1] = 10.0; state.posX[1] = 0.0; state.radius[1] = 8f
        state.names[2] = "C"; state.mass[2] = 1.0; state.posX[2] = 0.00001; state.radius[2] = 2f
        state.names[3] = "D"; state.mass[3] = 7.0; state.posX[3] = 25.0; state.radius[3] = 6f
        state.count = 4

        var absorbedNameReceived = ""
        var absorbedIndexReceived = -1
        var swappedIndexReceived = -1

        val collided = integrator.resolveCollisions(state) { absorbedIdx, swappedIdx, absorbedName, _, _, _, _ ->
            absorbedIndexReceived = absorbedIdx
            swappedIndexReceived = swappedIdx
            absorbedNameReceived = absorbedName
        }

        assertTrue(collided)
        assertEquals(3, state.count)

        assertEquals("C was absorbed", "C", absorbedNameReceived)
        assertEquals("Absorbed index was 2", 2, absorbedIndexReceived)
        assertEquals("Swapped index was 3", 3, swappedIndexReceived)

        // Slot 0: untouched A
        assertEquals("A", state.names[0])
        assertEquals(5.0, state.mass[0], 1e-9)

        // Slot 1: merged B
        assertEquals("B", state.names[1])
        assertEquals(11.0, state.mass[1], 1e-9)

        // Slot 2: swapped D (from slot 3)
        assertEquals("D", state.names[2])
        assertEquals(7.0, state.mass[2], 1e-9)
        assertEquals(25.0, state.posX[2], 1e-9)

        // Slot 3: vacated and zeroed out
        assertEquals("", state.names[3])
        assertEquals(0.0, state.mass[3], 1e-9)
        assertEquals(0.0, state.posX[3], 1e-9)
    }

    @Test
    fun cameraFollowAndTrailBufferSyncOnCompaction() {
        val cameraState = CameraState()
        val trailBuffer = OrbitalTrailBuffer(maxBodies = 4, trailCapacity = 10)

        // Populate mock trail for body 3 (D)
        trailBuffer.appendPoint(3, 100.0, 200.0)
        trailBuffer.appendPoint(3, 101.0, 201.0)
        assertEquals(2, trailBuffer.counts[3])

        // User is following body 3 (D)
        cameraState.followBody(3)
        assertTrue(cameraState.isFollowing)
        assertEquals(3, cameraState.followedBodyIndex)

        // Simulate body 2 absorbed and body 3 swapped into slot 2
        val absorbedIndex = 2
        val swappedIndex = 3

        if (cameraState.isFollowing) {
            if (cameraState.followedBodyIndex == absorbedIndex) {
                cameraState.stopFollowing()
            } else if (cameraState.followedBodyIndex == swappedIndex) {
                cameraState.followBody(absorbedIndex)
            }
        }
        trailBuffer.swapAndPop(absorbedIndex, swappedIndex)

        // Camera follow should now track index 2
        assertTrue(cameraState.isFollowing)
        assertEquals(2, cameraState.followedBodyIndex)

        // Trail buffer should have moved D's trail from slot 3 to slot 2
        assertEquals(2, trailBuffer.counts[2])
        assertEquals(0, trailBuffer.counts[3])
    }

    @Test
    fun distantBodiesDoNotCollideUnderUnitScaleConversion() {
        // Sun at (0, 0) with radius 109 Earth Radii
        state.names[0] = "Sun"
        state.mass[0] = 333000.0
        state.posX[0] = 0.0
        state.posY[0] = 0.0
        state.radius[0] = 109.0f

        // Mercury at 0.39 AU with radius 0.38 Earth Radii
        state.names[1] = "Mercury"
        state.mass[1] = 0.055
        state.posX[1] = 0.39
        state.posY[1] = 0.0
        state.radius[1] = 0.38f

        // Earth at 1.00 AU with radius 1.0 Earth Radii
        state.names[2] = "Earth"
        state.mass[2] = 1.0
        state.posX[2] = 1.0
        state.posY[2] = 0.0
        state.radius[2] = 1.0f

        state.count = 3

        val collided = integrator.resolveCollisions(state)
        assertFalse("Planets in normal orbits must NOT collide with the Sun or each other", collided)
        assertEquals("Count must remain 3", 3, state.count)
    }
}
