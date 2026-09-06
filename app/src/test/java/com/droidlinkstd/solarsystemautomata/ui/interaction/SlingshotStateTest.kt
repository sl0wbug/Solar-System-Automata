package com.droidlinkstd.solarsystemautomata.ui.interaction

import com.droidlinkstd.solarsystemautomata.domain.physics.SimulationEngine
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class SlingshotStateTest {

    private lateinit var slingshot: SlingshotState
    private lateinit var camera: CameraState

    @Before
    fun setUp() {
        slingshot = SlingshotState()
        camera = CameraState(
            initialCenterX = 0.0,
            initialCenterY = 0.0,
            initialZoom = 1.0f
        )
        camera.updateViewport(1000f, 1000f)
    }

    @Test
    fun startSlingshotInitializesWorldAndScreenCoordinates() {
        // Tap at screen (500, 500) -> world (0, 0)
        slingshot.startSlingshot(500f, 500f, camera)

        assertTrue(slingshot.isActive)
        assertEquals(500f, slingshot.originScreenX, 1e-4f)
        assertEquals(500f, slingshot.originScreenY, 1e-4f)
        assertEquals(0.0, slingshot.originWorldX, 1e-4)
        assertEquals(0.0, slingshot.originWorldY, 1e-4)
    }

    @Test
    fun calculateLaunchVelocityIsOppositeToDragDirection() {
        slingshot.startSlingshot(500f, 500f, camera)
        slingshot.velocitySensitivity = 1.0

        // Drag 100 pixels right (+X) and 50 pixels down (+Y)
        slingshot.updateDrag(600f, 550f)

        val vx = slingshot.calculateLaunchVelocityX(camera)
        val vy = slingshot.calculateLaunchVelocityY(camera)

        // Launch vector must be negative X (-100) and negative Y (-50)
        assertEquals(-100.0, vx, 1e-4)
        assertEquals(-50.0, vy, 1e-4)

        val expectedSpeed = sqrt(100.0 * 100.0 + 50.0 * 50.0)
        assertEquals(expectedSpeed, slingshot.calculateLaunchSpeed(camera), 1e-4)
    }

    @Test
    fun launchVelocityScalesWithCameraZoom() {
        slingshot.velocitySensitivity = 1.0

        // At zoom = 2.0, 100 pixels screen drag represents 50 units in world space
        camera.zoom = 2.0f
        slingshot.startSlingshot(500f, 500f, camera)
        slingshot.updateDrag(600f, 500f)

        val vx = slingshot.calculateLaunchVelocityX(camera)
        assertEquals(-50.0, vx, 1e-4)
    }

    @Test
    fun cancelResetsActiveState() {
        slingshot.startSlingshot(100f, 100f, camera)
        assertTrue(slingshot.isActive)

        slingshot.cancel()
        assertFalse(slingshot.isActive)
    }

    @Test
    fun spawnPresetsHaveCorrectAttributes() {
        assertEquals(0.005, SpawnPreset.ASTEROID.mass, 1e-6)
        assertEquals(1.0, SpawnPreset.PLANET.mass, 1e-6)
        assertEquals(317.8, SpawnPreset.GAS_GIANT.mass, 1e-6)
        assertEquals(10000.0, SpawnPreset.STAR.mass, 1e-6)

        assertTrue(SpawnPreset.STAR.radius > SpawnPreset.GAS_GIANT.radius)
        assertTrue(SpawnPreset.GAS_GIANT.radius > SpawnPreset.PLANET.radius)
        assertTrue(SpawnPreset.PLANET.radius > SpawnPreset.ASTEROID.radius)
    }

    @Test
    fun simulationEngineSpawnsBodyThreadSafely() {
        val engine = SimulationEngine(capacity = 3)
        assertEquals(0, engine.physicsState.count)

        val success1 = engine.spawnBody(
            name = "TestPlanet1",
            mass = 1.0,
            radius = 10f,
            color = 0xFFFFFFFF.toInt(),
            posX = 10.0,
            posY = 0.0,
            velX = 0.0,
            velY = 1.0
        )
        assertTrue(success1)
        assertEquals(1, engine.physicsState.count)
        assertEquals("TestPlanet1", engine.physicsState.names[0])
        assertEquals(10.0, engine.physicsState.posX[0], 1e-5)

        val success2 = engine.spawnBody("TestPlanet2", 2.0, 12f, 0, 20.0, 0.0, 0.0, 2.0)
        val success3 = engine.spawnBody("TestPlanet3", 3.0, 14f, 0, 30.0, 0.0, 0.0, 3.0)
        assertTrue(success2)
        assertTrue(success3)
        assertEquals(3, engine.physicsState.count)

        // Attempting to spawn at capacity = 3 must return false
        val success4 = engine.spawnBody("OverflowPlanet", 4.0, 16f, 0, 40.0, 0.0, 0.0, 4.0)
        assertFalse(success4)
        assertEquals(3, engine.physicsState.count)
    }
}
