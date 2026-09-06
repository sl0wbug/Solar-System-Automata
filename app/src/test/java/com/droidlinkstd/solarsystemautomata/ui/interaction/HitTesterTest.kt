package com.droidlinkstd.solarsystemautomata.ui.interaction

import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HitTesterTest {

    private lateinit var camera: CameraState
    private lateinit var snapshot: RenderSnapshot

    @Before
    fun setUp() {
        camera = CameraState(
            initialCenterX = 0.0,
            initialCenterY = 0.0,
            initialZoom = 1.0f
        )
        camera.updateViewport(1080f, 1920f)

        snapshot = RenderSnapshot(capacity = 4)
    }

    @Test
    fun returnsNegativeOneOnEmptySnapshot() {
        snapshot.count = 0
        val hitIndex = HitTester.findBodyAtScreenOffset(540f, 960f, snapshot, camera)
        assertEquals(-1, hitIndex)
    }

    @Test
    fun hitsBodyDirectlyAtCenter() {
        // Body 0 placed at world origin (0, 0), which projects to screen center (540, 960)
        snapshot.count = 1
        snapshot.posX[0] = 0.0
        snapshot.posY[0] = 0.0
        snapshot.radius[0] = 10f

        val hitIndex = HitTester.findBodyAtScreenOffset(540f, 960f, snapshot, camera)
        assertEquals(0, hitIndex)
    }

    @Test
    fun hitsBodyWithinTouchSlop() {
        // Body 0 at origin, screen (540, 960)
        snapshot.count = 1
        snapshot.posX[0] = 0.0
        snapshot.posY[0] = 0.0
        snapshot.radius[0] = 5f

        // Tap 20 pixels away (less than default touchSlop 36px)
        val hitIndex = HitTester.findBodyAtScreenOffset(560f, 960f, snapshot, camera, touchSlopPx = 36f)
        assertEquals(0, hitIndex)
    }

    @Test
    fun returnsNegativeOneWhenTapIsOutsideSlop() {
        // Body 0 at origin, screen (540, 960)
        snapshot.count = 1
        snapshot.posX[0] = 0.0
        snapshot.posY[0] = 0.0
        snapshot.radius[0] = 5f

        // Tap 50 pixels away (greater than touchSlop 36px)
        val hitIndex = HitTester.findBodyAtScreenOffset(595f, 960f, snapshot, camera, touchSlopPx = 36f)
        assertEquals(-1, hitIndex)
    }

    @Test
    fun selectsClosestBodyWhenMultipleWithinTouchSlop() {
        snapshot.count = 2
        // Body 0 at world (-15, 0) -> screen (525, 960)
        snapshot.posX[0] = -15.0
        snapshot.posY[0] = 0.0
        snapshot.radius[0] = 10f

        // Body 1 at world (10, 0) -> screen (550, 960)
        snapshot.posX[1] = 10.0
        snapshot.posY[1] = 0.0
        snapshot.radius[1] = 10f

        // Tap at (545, 960) -> distance to Body 1 is 5px, distance to Body 0 is 20px
        val hitIndex = HitTester.findBodyAtScreenOffset(545f, 960f, snapshot, camera, touchSlopPx = 36f)
        assertEquals(1, hitIndex)
    }
}
