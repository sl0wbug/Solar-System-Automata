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
 * Hill sphere containment, and numerical stability over 1,000 integration steps.
 */
class SolarSystemMoonsTest {

    private val integrator = OrbitalIntegrator()

    @Test
    fun testSolarSystemBodiesCountAndStructure() {
        val bodies = ScenarioPresets.SolarSystem.createBodies()
        // 1 Sun + 8 Planets + 12 Moons = 21 Celestial Bodies
        assertEquals("Total bodies in Solar System preset must be 21", 21, bodies.size)

        // Sun checks
        val sun = bodies.first { it.name == "Sun" }
        assertEquals(333000.0, sun.mass, 1e-9)
        assertEquals(109.0, sun.radius, 1e-9)
        assertEquals(0.0, sun.positionX, 1e-9)
        assertEquals(0.0, sun.positionY, 1e-9)
        assertEquals(0.0, sun.velocityX, 1e-9)
        assertEquals(0.0, sun.velocityY, 1e-9)

        // Expected planets
        val expectedPlanets = listOf(
            "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
        )
        for (planetName in expectedPlanets) {
            assertTrue("Planet $planetName must exist", bodies.any { it.name == planetName })
        }

        // Expected moons
        val expectedMoons = listOf(
            "Moon", "Phobos", "Deimos", "Io", "Europa", "Ganymede", "Callisto",
            "Titan", "Enceladus", "Titania", "Oberon", "Triton"
        )
        for (moonName in expectedMoons) {
            assertTrue("Moon $moonName must exist", bodies.any { it.name == moonName })
        }
    }

    @Test
    fun testPlanetaryCircularOrbitalEquilibrium() {
        val preset = ScenarioPresets.SolarSystem
        val bodies = preset.createBodies()
        val g = preset.g
        val sun = bodies.first { it.name == "Sun" }

        val planetNames = listOf(
            "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
        )

        for (name in planetNames) {
            val planet = bodies.first { it.name == name }
            val r = planet.positionX // All planets start along the +X axis
            val v = planet.velocityY // All planets have velocity strictly along +Y

            // Centripetal acceleration required for circular orbit: a_c = v^2 / r
            val aCentripetal = (v * v) / r

            // Gravitational acceleration from the Sun: a_g = G * M_sun / r^2
            val aGravity = (g * sun.mass) / (r * r)

            assertEquals(
                "Planet $name must be in exact circular equilibrium (a_c == a_g)",
                aGravity,
                aCentripetal,
                1e-9
            )
        }
    }

    @Test
    fun testMoonRelativeVelocitiesAndPositions() {
        val preset = ScenarioPresets.SolarSystem
        val bodies = preset.createBodies()
        val g = preset.g

        val pairings = listOf(
            "Earth" to listOf("Moon" to false),
            "Mars" to listOf("Phobos" to false, "Deimos" to false),
            "Jupiter" to listOf("Io" to false, "Europa" to false, "Ganymede" to false, "Callisto" to false),
            "Saturn" to listOf("Titan" to false, "Enceladus" to false),
            "Uranus" to listOf("Titania" to false, "Oberon" to false),
            "Neptune" to listOf("Triton" to true) // Triton has retrograde orbit
        )

        for ((planetName, moonSpecs) in pairings) {
            val planet = bodies.first { it.name == planetName }

            for ((moonName, isRetrograde) in moonSpecs) {
                val moon = bodies.first { it.name == moonName }
                val relDist = moon.positionX - planet.positionX
                assertTrue("Moon $moonName relative distance must be positive", relDist > 0.0)

                // Keplerian relative circular speed around parent planet: v_rel = sqrt(G * M_planet / r_rel)
                val expectedVrelMag = sqrt(g * planet.mass / relDist)
                val expectedVrel = if (isRetrograde) -expectedVrelMag else expectedVrelMag

                val expectedMoonVy = planet.velocityY + expectedVrel
                assertEquals(
                    "Moon $moonName velocity must strictly equal v_planet + v_rel",
                    expectedMoonVy,
                    moon.velocityY,
                    1e-9
                )

                // Velocity X must be 0 for bodies starting along X axis
                assertEquals(0.0, moon.velocityX, 1e-9)
            }
        }
    }

    @Test
    fun testHillSphereContainment() {
        val bodies = ScenarioPresets.SolarSystem.createBodies()
        val sun = bodies.first { it.name == "Sun" }

        val pairings = listOf(
            "Earth" to listOf("Moon"),
            "Mars" to listOf("Phobos", "Deimos"),
            "Jupiter" to listOf("Io", "Europa", "Ganymede", "Callisto"),
            "Saturn" to listOf("Titan", "Enceladus"),
            "Uranus" to listOf("Titania", "Oberon"),
            "Neptune" to listOf("Triton")
        )

        for ((planetName, moonNames) in pairings) {
            val planet = bodies.first { it.name == planetName }
            val a = planet.positionX
            // Hill radius: r_H = a * cbrt(m_planet / (3 * M_sun))
            val hillRadius = a * cbrt(planet.mass / (3.0 * sun.mass))

            for (moonName in moonNames) {
                val moon = bodies.first { it.name == moonName }
                val relDist = moon.positionX - planet.positionX

                assertTrue(
                    "Moon $moonName ($relDist AU) must reside inside $planetName Hill sphere ($hillRadius AU)",
                    relDist < hillRadius
                )
            }
        }
    }

    @Test
    fun testIntegrate1000Steps_zeroCollisionsAndBoundedEnergy() {
        val preset = ScenarioPresets.SolarSystem
        val bodies = preset.createBodies()
        val state = PhysicsState(capacity = bodies.size + 10)
        state.loadFromDomain(bodies)

        val dt = 0.001
        val g = preset.g
        val softening = preset.softening

        integrator.computeAccelerations(state, g, softening)

        val initialEnergy = state.calculateTotalMechanicalEnergy(g, softening)
        var collisionOccurred = false

        val listener = OrbitalIntegrator.CollisionListener { _, _, absorbedName, _, _, _, _ ->
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
        assertEquals("Count must remain 21", 21, state.count)

        val finalEnergy = state.calculateTotalMechanicalEnergy(g, softening)
        val energyDrift = abs((finalEnergy - initialEnergy) / initialEnergy)

        // Energy drift under symplectic Velocity Verlet must remain bounded (< 0.5% over 1,000 steps)
        assertTrue("Energy drift ($energyDrift) must remain bounded (< 0.005)", energyDrift < 0.005)
    }
}
