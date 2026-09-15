package com.droidlinkstd.solarsystemautomata.domain.physics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Unit test suite verifying the mathematical formulations and state swap behavior
 * for all canonical scenario presets.
 */
class ScenarioPresetsTest {

    private val integrator = OrbitalIntegrator()

    @Test
    fun testAllPresetsExistAndAreConfigured() {
        val presets = ScenarioPresets.ALL_PRESETS
        assertEquals(5, presets.size)

        for (preset in presets) {
            val bodies = preset.createBodies()
            assertTrue("Preset ${preset.title} must have at least 2 bodies", bodies.size >= 2)
            assertTrue("G must be positive", preset.g > 0.0)
            assertTrue("Softening must be non-negative", preset.softening >= 0.0)
            assertNotNull(preset.title)
            assertNotNull(preset.subtitle)
        }
    }

    @Test
    fun testFigureEight_conservesLinearMomentumAndBarycenter() {
        val bodies = ScenarioPresets.FigureEight.createBodies()
        assertEquals(3, bodies.size)

        var totalMass = 0.0
        var cmX = 0.0
        var cmY = 0.0
        var pX = 0.0
        var pY = 0.0

        for (body in bodies) {
            totalMass += body.mass
            cmX += body.mass * body.positionX
            cmY += body.mass * body.positionY
            pX += body.mass * body.velocityX
            pY += body.mass * body.velocityY
        }

        cmX /= totalMass
        cmY /= totalMass

        // Center of mass must be at the origin within numerical tolerance
        assertEquals("Figure-8 Center of Mass X must be 0", 0.0, cmX, 1e-6)
        assertEquals("Figure-8 Center of Mass Y must be 0", 0.0, cmY, 1e-6)

        // Net momentum must be strictly zero
        assertEquals("Figure-8 Net Px must be 0", 0.0, pX, 1e-6)
        assertEquals("Figure-8 Net Py must be 0", 0.0, pY, 1e-6)

        // Integrate for 200 steps and verify momentum conservation
        val state = PhysicsState(capacity = 3)
        state.loadFromDomain(bodies)
        val dt = 0.001
        val g = 1.0
        val softening = 0.0001

        integrator.computeAccelerations(state, g, softening)
        for (step in 0 until 200) {
            integrator.step(state, dt, g, softening)
        }

        var pStepX = 0.0
        var pStepY = 0.0
        for (i in 0 until state.count) {
            pStepX += state.mass[i] * state.velX[i]
            pStepY += state.mass[i] * state.velY[i]
        }

        assertEquals("Figure-8 Net Px after 200 steps must remain 0", 0.0, pStepX, 1e-5)
        assertEquals("Figure-8 Net Py after 200 steps must remain 0", 0.0, pStepY, 1e-5)
    }

    @Test
    fun testBinaryStar_barycenterAndCentripetalVelocity() {
        val bodies = ScenarioPresets.BinaryStar.createBodies()
        assertEquals(3, bodies.size)

        val star1 = bodies[0]
        val star2 = bodies[1]
        val planet = bodies[2]

        // Binary stars have equal mass and symmetric distance
        assertEquals(star1.mass, star2.mass, 1e-9)
        assertEquals(0.0, star1.positionX + star2.positionX, 1e-9)

        // Center of mass of the binary pair is at the origin
        val starCmX = (star1.mass * star1.positionX + star2.mass * star2.positionX) / (star1.mass + star2.mass)
        val starCmY = (star1.mass * star1.positionY + star2.mass * star2.positionY) / (star1.mass + star2.mass)
        assertEquals(0.0, starCmX, 1e-9)
        assertEquals(0.0, starCmY, 1e-9)

        // Total momentum of binary pair is zero
        val starPx = star1.mass * star1.velocityX + star2.mass * star2.velocityX
        val starPy = star1.mass * star1.velocityY + star2.mass * star2.velocityY
        assertEquals(0.0, starPx, 1e-9)
        assertEquals(0.0, starPy, 1e-9)

        // Circumbinary planet is at safe distance (> 3 * separation = 12 AU)
        val planetDist = hypot(planet.positionX, planet.positionY)
        assertTrue("Planet distance ($planetDist) must be >= 12 AU", planetDist >= 12.0)
    }

    @Test
    fun testLagrangePoints_equilateralTriangleGeometry() {
        val bodies = ScenarioPresets.LagrangePoints.createBodies()
        assertEquals(4, bodies.size)

        val star = bodies[0]
        val planet = bodies[1]
        val trojanL4 = bodies[2]

        val dStarPlanet = hypot(planet.positionX - star.positionX, planet.positionY - star.positionY)
        val dStarL4 = hypot(trojanL4.positionX - star.positionX, trojanL4.positionY - star.positionY)
        val dPlanetL4 = hypot(trojanL4.positionX - planet.positionX, trojanL4.positionY - planet.positionY)

        // Equilateral triangle: d(Star, Planet) == d(Star, L4) == d(Planet, L4)
        assertEquals("Distance Star-Planet", 10.0, dStarPlanet, 1e-4)
        assertEquals("Distance Star-L4", 10.0, dStarL4, 1e-4)
        assertEquals("Distance Planet-L4", 10.0, dPlanetL4, 1e-4)

        // Primary star and planet barycenter momentum check
        val starPlanetPx = star.mass * star.velocityX + planet.mass * planet.velocityX
        val starPlanetPy = star.mass * star.velocityY + planet.mass * planet.velocityY
        assertEquals(0.0, starPlanetPx, 1e-6)
        assertEquals(0.0, starPlanetPy, 1e-6)
    }

    @Test
    fun testChaoticThreeBody_pythagoreanGeometryAndZeroInitialMomentum() {
        val bodies = ScenarioPresets.ChaoticThreeBody.createBodies()
        assertEquals(3, bodies.size)

        val b1 = bodies[0] // m = 300 at (1, 3)
        val b2 = bodies[1] // m = 400 at (-2, -1)
        val b3 = bodies[2] // m = 500 at (1, -1)

        // Verify 3-4-5 right triangle side lengths:
        // d(2, 3) = sqrt((1 - (-2))^2 + (-1 - (-1))^2) = 3
        val d23 = hypot(b3.positionX - b2.positionX, b3.positionY - b2.positionY)
        assertEquals("Side length 3", 3.0, d23, 1e-9)

        // d(1, 3) = sqrt((1 - 1)^2 + (3 - (-1))^2) = 4
        val d13 = hypot(b3.positionX - b1.positionX, b3.positionY - b1.positionY)
        assertEquals("Side length 4", 4.0, d13, 1e-9)

        // d(1, 2) = sqrt((-2 - 1)^2 + (-1 - 3)^2) = 5
        val d12 = hypot(b2.positionX - b1.positionX, b2.positionY - b1.positionY)
        assertEquals("Hypotenuse length 5", 5.0, d12, 1e-9)

        // Center of mass must be exactly at origin (0, 0)
        val totalM = b1.mass + b2.mass + b3.mass
        val cmX = (b1.mass * b1.positionX + b2.mass * b2.positionX + b3.mass * b3.positionX) / totalM
        val cmY = (b1.mass * b1.positionY + b2.mass * b2.positionY + b3.mass * b3.positionY) / totalM
        assertEquals(0.0, cmX, 1e-9)
        assertEquals(0.0, cmY, 1e-9)

        // All bodies initially at rest
        for (b in bodies) {
            assertEquals(0.0, b.velocityX, 1e-9)
            assertEquals(0.0, b.velocityY, 1e-9)
        }
    }

    @Test
    fun testPhysicsState_resetClearsBuffersInPlace() {
        val state = PhysicsState(capacity = 8)
        state.loadFromDomain(ScenarioPresets.FigureEight.createBodies())
        assertEquals(3, state.count)

        // Reset in-place
        state.reset()
        assertEquals(0, state.count)
        assertEquals(0.0, state.posX[0], 1e-9)
        assertEquals(0.0, state.mass[0], 1e-9)
        assertEquals("", state.names[0])
    }

    @Test
    fun testSimulationEngine_loadScenarioUpdatesStateAndSnapshot() {
        val engine = SimulationEngine(capacity = 16)
        assertEquals(0, engine.getRenderSnapshot().count)

        engine.loadScenario(ScenarioPresets.BinaryStar)
        assertEquals(ScenarioPresetId.BINARY_STAR, engine.currentPreset.id)

        val snapshot = engine.getRenderSnapshot()
        assertEquals(3, snapshot.count)
        assertEquals("Sol-Alpha", snapshot.names[0])
        assertEquals("Sol-Beta", snapshot.names[1])
        assertEquals("Tatooine", snapshot.names[2])

        // Switch to Chaotic 3-Body
        engine.loadScenario(ScenarioPresets.ChaoticThreeBody)
        assertEquals(ScenarioPresetId.CHAOTIC_THREE_BODY, engine.currentPreset.id)
        val snapshot2 = engine.getRenderSnapshot()
        assertEquals(3, snapshot2.count)
        assertEquals("Body 3 (m=3)", snapshot2.names[0])
    }
}
