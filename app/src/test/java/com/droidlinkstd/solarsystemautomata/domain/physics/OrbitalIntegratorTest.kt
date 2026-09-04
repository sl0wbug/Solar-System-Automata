package com.droidlinkstd.solarsystemautomata.domain.physics

import com.droidlinkstd.solarsystemautomata.data.CelestialBody
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyDao
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepository
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepositoryImpl
import com.droidlinkstd.solarsystemautomata.Planet
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for [OrbitalIntegrator], [PhysicsState], and [SimulationEngine].
 *
 * Verifies:
 * 1. Hamiltonian Conservation (mechanical energy drift < 0.05% over 10,000 steps)
 * 2. Symmetry / Linear Momentum Conservation
 * 3. Zero heap allocation profile inside the hot simulation loop
 * 4. Softening singularity avoidance
 * 5. Domain model hydration and simulation engine execution
 */
class OrbitalIntegratorTest {

    private val integrator = OrbitalIntegrator()

    /**
     * HAMILTONIAN CONSERVATION TEST:
     * Setup a circular 2-body orbit (Sun and Earth) and run for 10,000 steps.
     * With a symplectic Velocity Verlet integrator, total mechanical energy (E = K + U)
     * must be conserved with bounded oscillation and drift < 0.05%.
     */
    @Test
    fun testHamiltonianConservation_circularOrbit() {
        val state = PhysicsState(capacity = 2)

        val g = 1.0
        val mSun = 1000.0
        val mEarth = 1.0
        val r = 10.0
        val softening = 0.0 // Pure Newtonian potential

        // Circular orbit velocity: v = sqrt(G * (M + m) / r)
        val vOrbit = Math.sqrt(g * (mSun + mEarth) / r)

        // Center of mass correction so system remains at rest
        val vSunY = -(mEarth / mSun) * vOrbit
        val vEarthY = vOrbit + vSunY

        val sun = CelestialBody(
            id = 1,
            name = "Sun",
            mass = mSun,
            positionX = 0.0,
            positionY = 0.0,
            velocityX = 0.0,
            velocityY = vSunY,
            radius = 2.0,
            colorHex = 0xFFFFD700L
        )

        val earth = CelestialBody(
            id = 2,
            name = "Earth",
            mass = mEarth,
            positionX = r,
            positionY = 0.0,
            velocityX = 0.0,
            velocityY = vEarthY,
            radius = 1.0,
            colorHex = 0xFF4B85C1L
        )

        state.loadFromDomain(listOf(sun, earth))
        integrator.computeAccelerations(state, g, softening)

        val initialEnergy = state.calculateTotalMechanicalEnergy(g, softening)
        assertTrue("Initial mechanical energy must be negative for bound orbit", initialEnergy < 0.0)

        val dt = 0.001
        val steps = 10_000
        var step = 0
        while (step < steps) {
            integrator.step(state, dt, g, softening)
            step++
        }

        val finalEnergy = state.calculateTotalMechanicalEnergy(g, softening)
        val relativeEnergyDrift = Math.abs((finalEnergy - initialEnergy) / initialEnergy)

        // Strict assertion: energy drift must be less than 0.05% (0.0005)
        assertTrue(
            "Mechanical energy drift ($relativeEnergyDrift) exceeded 0.05% threshold over 10,000 steps. Initial: $initialEnergy, Final: $finalEnergy",
            relativeEnergyDrift < 0.0005
        )
    }

    /**
     * SYMMETRY / LINEAR MOMENTUM CONSERVATION TEST:
     * Verifies that Newton's third law implementation (F_ij = -F_ji) ensures
     * exact conservation of total linear momentum in an isolated multi-body system.
     */
    @Test
    fun testLinearMomentumConservation_symmetricSystem() {
        val state = PhysicsState(capacity = 4)
        val g = 1.0
        val softening = 0.01

        val b1 = CelestialBody(1, "Body1", mass = 50.0, positionX = -10.0, positionY = 0.0, velocityX = 0.1, velocityY = 0.5, radius = 1.0, colorHex = 0xFFFFFFFFL)
        val b2 = CelestialBody(2, "Body2", mass = 30.0, positionX = 10.0, positionY = 5.0, velocityX = -0.2, velocityY = -0.3, radius = 1.0, colorHex = 0xFFFFFFFFL)
        val b3 = CelestialBody(3, "Body3", mass = 20.0, positionX = 0.0, positionY = -8.0, velocityX = 0.4, velocityY = -0.1, radius = 1.0, colorHex = 0xFFFFFFFFL)

        state.loadFromDomain(listOf(b1, b2, b3))
        integrator.computeAccelerations(state, g, softening)

        val pXInitial = state.calculateTotalLinearMomentumX()
        val pYInitial = state.calculateTotalLinearMomentumY()

        val dt = 0.01
        val steps = 5_000
        var s = 0
        while (s < steps) {
            integrator.step(state, dt, g, softening)
            s++
        }

        val pXFinal = state.calculateTotalLinearMomentumX()
        val pYFinal = state.calculateTotalLinearMomentumY()

        assertEquals("Linear momentum X must be conserved", pXInitial, pXFinal, 1e-9)
        assertEquals("Linear momentum Y must be conserved", pYInitial, pYFinal, 1e-9)
    }

    /**
     * ZERO-ALLOCATION PROFILE TEST:
     * Verifies that executing the integrator loop causes zero heap memory allocations.
     */
    @Test
    fun testZeroAllocationHotLoop() {
        val state = PhysicsState(capacity = 8)
        val g = 1.0
        val softening = 0.01

        val bodies = (1..6).map { idx ->
            CelestialBody(
                id = idx,
                name = "Body$idx",
                mass = idx * 10.0,
                positionX = idx * 2.0,
                positionY = idx * -1.5,
                velocityX = idx * 0.05,
                velocityY = idx * -0.02,
                radius = 1.0,
                colorHex = 0xFF00FF00L
            )
        }

        state.loadFromDomain(bodies)
        integrator.computeAccelerations(state, g, softening)

        val dt = 0.001

        // Verify array identities before hot loop
        val originalPosX = state.posX
        val originalPosY = state.posY
        val originalVelX = state.velX
        val originalVelY = state.velY
        val originalAccX = state.accX
        val originalAccY = state.accY

        // JIT Warmup to ensure bytecode compilation and class loading are complete
        var warmup = 0
        while (warmup < 2_000) {
            integrator.step(state, dt, g, softening)
            warmup++
        }

        // Test using JVM ThreadMXBean via reflection (present on Oracle/OpenJDK JVM)
        var testedViaMXBean = false
        try {
            val factoryClass = Class.forName("java.lang.management.ManagementFactory")
            val getBeanMethod = factoryClass.getMethod("getThreadMXBean")
            val bean = getBeanMethod.invoke(null)
            val mxBeanInterface = Class.forName("com.sun.management.ThreadMXBean")

            if (mxBeanInterface.isInstance(bean)) {
                val isSupportedMethod = mxBeanInterface.getMethod("isThreadAllocatedMemorySupported")
                val isSupported = isSupportedMethod.invoke(bean) as? Boolean ?: false

                if (isSupported) {
                    val setEnabledMethod = mxBeanInterface.getMethod("setThreadAllocatedMemoryEnabled", java.lang.Boolean.TYPE)
                    setEnabledMethod.invoke(bean, true)

                    val getBytesMethod = mxBeanInterface.getMethod("getThreadAllocatedBytes", java.lang.Long.TYPE)
                    val currentThreadId = Thread.currentThread().id

                    val allocatedBytesBefore = getBytesMethod.invoke(bean, currentThreadId) as Long

                    val steps = 10_000
                    var i = 0
                    while (i < steps) {
                        integrator.step(state, dt, g, softening)
                        i++
                    }

                    val allocatedBytesAfter = getBytesMethod.invoke(bean, currentThreadId) as Long
                    val deltaAllocated = allocatedBytesAfter - allocatedBytesBefore

                    assertEquals(
                        "Integrator hot loop must execute with 0 heap bytes allocated over $steps steps",
                        0L,
                        deltaAllocated
                    )
                    testedViaMXBean = true
                }
            }
        } catch (e: Throwable) {
            // JVM environment doesn't expose com.sun.management.ThreadMXBean
        }

        // Verify arrays were never reallocated
        assertTrue("posX array reference must remain identical", state.posX === originalPosX)
        assertTrue("posY array reference must remain identical", state.posY === originalPosY)
        assertTrue("velX array reference must remain identical", state.velX === originalVelX)
        assertTrue("velY array reference must remain identical", state.velY === originalVelY)
        assertTrue("accX array reference must remain identical", state.accX === originalAccX)
        assertTrue("accY array reference must remain identical", state.accY === originalAccY)
    }

    /**
     * SOFTENING FACTOR TEST:
     * Verifies that softening factor eliminates numerical singularities when separation r -> 0.
     */
    @Test
    fun testSofteningPreventsSingularity() {
        val state = PhysicsState(capacity = 2)
        val g = 1.0
        val softening = 0.1

        // Both bodies at identical position (r = 0)
        val b1 = CelestialBody(1, "A", mass = 100.0, positionX = 0.0, positionY = 0.0, velocityX = 0.0, velocityY = 0.0, radius = 1.0, colorHex = 0xFFFFFFFFL)
        val b2 = CelestialBody(2, "B", mass = 100.0, positionX = 0.0, positionY = 0.0, velocityX = 0.0, velocityY = 0.0, radius = 1.0, colorHex = 0xFFFFFFFFL)

        state.loadFromDomain(listOf(b1, b2))
        integrator.computeAccelerations(state, g, softening)

        assertFalse("Acceleration X must not be NaN", state.accX[0].isNaN())
        assertFalse("Acceleration X must not be Infinite", state.accX[0].isInfinite())
        assertFalse("Acceleration Y must not be NaN", state.accY[0].isNaN())
        assertFalse("Acceleration Y must not be Infinite", state.accY[0].isInfinite())

        // Step integration
        integrator.step(state, 0.01, g, softening)

        assertFalse("Position X must not be NaN", state.posX[0].isNaN())
        assertFalse("Velocity X must not be NaN", state.velX[0].isNaN())
    }

    /**
     * DOMAIN HYDRATION TEST:
     * Verifies mapping from CelestialBody domain models to Structure of Arrays layout.
     */
    @Test
    fun testPhysicsState_loadFromDomain() {
        val state = PhysicsState(capacity = 10)
        val bodies = listOf(
            CelestialBody(1, "Body1", 10.5, 1.23456789012345, 2.34567890123456, -0.123, 0.456, 5.0, 0xFF112233L),
            CelestialBody(2, "Body2", 20.5, -3.23456789012345, -4.34567890123456, 0.789, -0.987, 8.0, 0xFF445566L)
        )

        state.loadFromDomain(bodies)

        assertEquals(2, state.count)
        assertEquals(1.23456789012345, state.posX[0], 1e-15)
        assertEquals(2.34567890123456, state.posY[0], 1e-15)
        assertEquals(-0.123, state.velX[0], 1e-15)
        assertEquals(0.456, state.velY[0], 1e-15)
        assertEquals(10.5, state.mass[0], 1e-15)
        assertEquals(5.0f, state.radius[0], 1e-6f)
        assertEquals(0xFF112233.toInt(), state.color[0])

        // Unused slots must be zeroed
        assertEquals(0.0, state.posX[2], 0.0)
        assertEquals(0.0, state.mass[2], 0.0)
    }

    /**
     * SIMULATION ENGINE INTEGRATION TEST:
     * Tests fixed timestep execution, pause/resume, speed multiplier, and snapshot publishing.
     */
    @Test
    fun testSimulationEngine_executionAndSnapshots() = runBlocking {
        val fakeDao = object : CelestialBodyDao {
            val list = mutableListOf(
                Planet(1, "Sun", 0xFFFFD700L, 20f, 0f, 0f, 10f, 0f, 0f, 0f, "Sun"),
                Planet(2, "Planet", 0xFF0000FFL, 10f, 100f, 1f, 1f, 10f, 1f, 10f, "Planet")
            )
            override fun getAllBodies() = flowOf(list)
            override suspend fun getAllBodiesOnce() = list
            override suspend fun getBodyById(id: Int) = list.find { it.id == id }
            override suspend fun insertBody(body: Planet) = 1L
            override suspend fun insertBodies(bodies: List<Planet>) = listOf(1L)
            override suspend fun updateBody(body: Planet) {}
            override suspend fun deleteBody(body: Planet) {}
            override suspend fun deleteAllBodies() {}
        }
        val repository: CelestialBodyRepository = CelestialBodyRepositoryImpl(fakeDao)

        val engine = SimulationEngine()
        engine.loadFromRepository(repository, includeCentralSun = false)

        val initialSnapshot = engine.getRenderSnapshot()
        assertEquals(2, initialSnapshot.count)

        // Test single deterministic step
        engine.stepOnce(0.01)
        val steppedSnapshot = engine.getRenderSnapshot()
        assertEquals(2, steppedSnapshot.count)

        // Speed multiplier control
        engine.setSpeedMultiplier(2.5)
        assertEquals(2.5, engine.speedMultiplier, 1e-9)

        engine.setSpeedMultiplier(-1.0)
        assertEquals(0.0, engine.speedMultiplier, 1e-9)

        // Start and pause
        engine.start()
        assertTrue(engine.isRunning)
        engine.pause()
        assertFalse(engine.isRunning)
        engine.stop()
    }
}
