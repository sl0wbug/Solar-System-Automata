package com.droidlinkstd.solarsystemautomata.domain.physics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Unit test suite verifying the astrophysical and engineering invariants of the
 * Solar System preset, including G calibration, circular equilibrium, moon velocities,
 * Hill sphere containment, momentum conservation, and numerical stability over 1,000 integration steps.
 */
class SolarSystemMoonsTest {

    private val integrator = OrbitalIntegrator()

    @Test
    fun testSolarSystemBodiesCountAndStructure() {
        val bodies = ScenarioPresets.SolarSystem.createBodies()
        // 1 Sun + 8 Planets + 288 Confirmed Moons = 297 Celestial Bodies
        assertEquals("Total bodies in Solar System preset must be 297", 297, bodies.size)

        // Sun checks
        val sun = bodies.first { it.name == "Sun" }
        assertEquals(333000.0, sun.mass, 1e-9)
        assertEquals(109.0, sun.radius, 1e-9)
        assertTrue("Sun positionX must be balanced near barycenter (< 0.02 AU)", abs(sun.positionX) < 0.02)
        assertTrue("Sun positionY must be balanced near barycenter (< 0.02 AU)", abs(sun.positionY) < 0.02)
        assertTrue("Sun velocityX must be balanced counter-velocity (< 0.001 AU/s)", abs(sun.velocityX) < 0.001)
        assertTrue("Sun velocityY must be balanced counter-velocity (< 0.001 AU/s)", abs(sun.velocityY) < 0.001)

        // Net system linear momentum must be strictly zero (conserved system)
        var totalPx = 0.0
        var totalPy = 0.0
        for (b in bodies) {
            totalPx += b.mass * b.velocityX
            totalPy += b.mass * b.velocityY
        }
        assertEquals("Net system Px must be 0", 0.0, totalPx, 1e-9)
        assertEquals("Net system Py must be 0", 0.0, totalPy, 1e-9)

        // Expected planets
        val expectedPlanets = listOf(
            "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
        )
        for (planetName in expectedPlanets) {
            assertTrue("Planet $planetName must exist", bodies.any { it.name == planetName })
        }

        // Expected key major moons across the solar system
        val sampleMoons = listOf(
            "Moon", "Phobos", "Deimos",
            "Io", "Europa", "Ganymede", "Callisto", "Amalthea", "Himalia", "Ananke", "Carme", "Pasiphae",
            "Mimas", "Enceladus", "Tethys", "Dione", "Rhea", "Titan", "Hyperion", "Iapetus", "Phoebe", "Pan",
            "Miranda", "Ariel", "Umbriel", "Titania", "Oberon", "Puck", "Sycorax",
            "Proteus", "Triton", "Nereid", "Naiad"
        )
        for (moonName in sampleMoons) {
            assertTrue("Sample moon $moonName must exist", bodies.any { it.name == moonName })
        }
    }

    @Test
    fun testPlanetaryCircularOrbitalEquilibrium() {
        val preset = ScenarioPresets.SolarSystem
        val bodies = preset.createBodies()
        val g = preset.g
        val sunMass = 333000.0

        val planetNames = listOf(
            "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
        )

        for (name in planetNames) {
            val planet = bodies.first { it.name == name }
            val r = hypot(planet.positionX, planet.positionY)
            val v = hypot(planet.velocityX, planet.velocityY)

            // Centripetal acceleration required for circular orbit: a_c = v^2 / r
            val aCentripetal = (v * v) / r

            // Gravitational acceleration from the Sun: a_g = G * M_sun / r^2
            val aGravity = (g * sunMass) / (r * r)

            assertEquals(
                "Planet $name must be in exact circular equilibrium (a_c == a_g)",
                aGravity,
                aCentripetal,
                1e-9
            )
        }
    }

    @Test
    fun testAll288MoonsRelativeVelocitiesAndHillSphereContainment() {
        val preset = ScenarioPresets.SolarSystem
        val bodies = preset.createBodies()
        val g = preset.g
        val sun = bodies.first { it.name == "Sun" }

        val planetMoonCounts = listOf(
            "Mercury" to 0,
            "Venus" to 0,
            "Earth" to 1,
            "Mars" to 2,
            "Jupiter" to 95,
            "Saturn" to 146,
            "Uranus" to 28,
            "Neptune" to 16
        )

        var totalMoonsFound = 0
        var bodyIdx = 1 // Skip Sun at index 0

        for ((planetName, expectedMoonCount) in planetMoonCounts) {
            val planet = bodies[bodyIdx]
            assertEquals("Planet at expected index must match $planetName", planetName, planet.name)
            bodyIdx++

            val a = hypot(planet.positionX, planet.positionY)
            val hillRadius = a * cbrt(planet.mass / (3.0 * sun.mass))

            for (m in 0 until expectedMoonCount) {
                val moon = bodies[bodyIdx]
                bodyIdx++
                totalMoonsFound++

                val relDist = hypot(moon.positionX - planet.positionX, moon.positionY - planet.positionY)
                assertTrue("Moon ${moon.name} relDist ($relDist AU) must be positive", relDist > 0.0)

                // Must reside inside planet's Hill sphere
                assertTrue(
                    "Moon ${moon.name} ($relDist AU) must reside inside $planetName Hill sphere ($hillRadius AU)",
                    relDist < hillRadius
                )

                // Must have exact circular Keplerian relative speed
                val expectedVrelMag = sqrt(g * planet.mass / relDist)
                val actualVrelMag = hypot(moon.velocityX - planet.velocityX, moon.velocityY - planet.velocityY)

                assertEquals(
                    "Moon ${moon.name} relative speed must strictly equal Keplerian circular speed",
                    expectedVrelMag,
                    actualVrelMag,
                    1e-9
                )
            }
        }

        assertEquals("Total natural satellites across solar system must be exactly 288", 288, totalMoonsFound)
        assertEquals("All 297 bodies accounted for", 297, bodyIdx)
    }

    @Test
    fun testIntegrate1000Steps_zeroCollisionsAndBoundedEnergy() {
        val preset = ScenarioPresets.SolarSystem
        val bodies = preset.createBodies()
        val state = PhysicsState(capacity = 512)
        state.loadFromDomain(bodies)

        val dt = 0.001
        val g = preset.g
        val softening = preset.softening

        integrator.computeAccelerations(state, g, softening)

        val initialEnergy = state.calculateTotalMechanicalEnergy(g, softening)
        var collisionOccurred = false

        val listener = OrbitalIntegrator.CollisionListener { _, _, _, _, _, _, _ ->
            collisionOccurred = true
        }

        // Integrate 1,000 steps (1.0 second simulation time)
        for (step in 0 until 1000) {
            integrator.step(state, dt, g, softening)
            val hadCollision = integrator.resolveCollisions(state, listener)
            if (hadCollision) {
                collisionOccurred = true
            }
        }

        assertFalse("No celestial bodies or moons should collide or merge during 1,000 steps", collisionOccurred)
        assertEquals("Count must remain 297", 297, state.count)

        val finalEnergy = state.calculateTotalMechanicalEnergy(g, softening)
        val energyDrift = abs((finalEnergy - initialEnergy) / initialEnergy)

        // Energy drift under symplectic Velocity Verlet must remain bounded (< 0.5% over 1,000 steps)
        assertTrue("Energy drift ($energyDrift) must remain bounded (< 0.005)", energyDrift < 0.005)
    }
}
