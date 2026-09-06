package com.droidlinkstd.solarsystemautomata.ui.camera

import androidx.compose.ui.geometry.Offset
import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class CameraStateTest {

    private lateinit var camera: CameraState

    @Before
    fun setUp() {
        camera = CameraState(
            initialCenterX = 0.0,
            initialCenterY = 0.0,
            initialZoom = 1.0f
        )
        camera.updateViewport(1080f, 1920f)
    }

    @Test
    fun worldToScreenAndScreenToWorldAreExactInverses() {
        // Test with astronomical coordinates (~ 1 AU = 1.496e11 meters)
        val testWorldX = 1.495978707e11
        val testWorldY = -7.785e10
        camera.centerX = 1.0e11
        camera.centerY = 0.0
        camera.zoom = 1e-9f // 1 pixel = 1e9 meters

        val screenX = camera.worldToScreenX(testWorldX)
        val screenY = camera.worldToScreenY(testWorldY)

        val invertedWorldX = camera.screenToWorldX(screenX)
        val invertedWorldY = camera.screenToWorldY(screenY)

        // Tolerance for double precision after float screen pixel projection
        // With zoom = 1e-9, 1 screen pixel = 1e9 m, float has 24 bits ~ 1 part in 1.6e7
        val relativeDiffX = abs(testWorldX - invertedWorldX) / testWorldX
        val relativeDiffY = abs(testWorldY - invertedWorldY) / abs(testWorldY)

        assertTrue("Expected relative difference < 1e-6, got $relativeDiffX", relativeDiffX < 1e-6)
        assertTrue("Expected relative difference < 1e-6, got $relativeDiffY", relativeDiffY < 1e-6)
    }

    @Test
    fun panByShiftsWorldCenterCorrectly() {
        camera.centerX = 0.0
        camera.centerY = 0.0
        camera.zoom = 2.0f

        // Pan 100 screen pixels right, 200 down
        camera.panBy(100f, 200f)

        assertEquals(-50.0, camera.centerX, 1e-5)
        assertEquals(-100.0, camera.centerY, 1e-5)
    }

    @Test
    fun zoomByPreservesFocalPointInvariant() {
        camera.centerX = 500.0
        camera.centerY = -300.0
        camera.zoom = 1.5f

        // Pinch centroid at off-center screen location
        val centroid = Offset(800f, 600f)

        // Find world coordinate under centroid BEFORE zoom
        val worldUnderCentroidX = camera.screenToWorldX(centroid.x)
        val worldUnderCentroidY = camera.screenToWorldY(centroid.y)

        // Zoom in by factor of 2.5
        camera.zoomBy(centroid, 2.5f)

        // Project the invariant world coordinate to screen AFTER zoom
        val screenAfterZoomX = camera.worldToScreenX(worldUnderCentroidX)
        val screenAfterZoomY = camera.worldToScreenY(worldUnderCentroidY)

        assertEquals(centroid.x, screenAfterZoomX, 0.01f)
        assertEquals(centroid.y, screenAfterZoomY, 0.01f)
    }

    @Test
    fun fitBoundsCentersAndFitsWithinViewport() {
        val minX = -1000.0
        val minY = -2000.0
        val maxX = 1000.0
        val maxY = 2000.0

        camera.fitBounds(minX, minY, maxX, maxY, paddingPx = 40f)

        assertEquals(0.0, camera.centerX, 1e-5)
        assertEquals(0.0, camera.centerY, 1e-5)

        // Verify bounding points are within viewport
        val screenMinX = camera.worldToScreenX(minX)
        val screenMaxX = camera.worldToScreenX(maxX)
        val screenMinY = camera.worldToScreenY(minY)
        val screenMaxY = camera.worldToScreenY(maxY)

        assertTrue(screenMinX >= 40f)
        assertTrue(screenMaxX <= 1080f - 40f)
        assertTrue(screenMinY >= 40f)
        assertTrue(screenMaxY <= 1920f - 40f)
    }

    @Test
    fun followModeSyncsCameraCenterToFollowedBody() {
        val snapshot = RenderSnapshot(capacity = 2)
        snapshot.count = 1
        snapshot.posX[0] = 4200.0
        snapshot.posY[0] = -8500.0

        camera.followBody(0)
        assertTrue(camera.isFollowing)
        assertEquals(0, camera.followedBodyIndex)

        camera.updateFollow(snapshot)
        assertEquals(4200.0, camera.centerX, 1e-5)
        assertEquals(-8500.0, camera.centerY, 1e-5)
    }

    @Test
    fun panByDisengagesFollowMode() {
        camera.followBody(2)
        assertTrue(camera.isFollowing)

        camera.panBy(50f, 50f)
        assertFalse(camera.isFollowing)
        assertEquals(-1, camera.followedBodyIndex)
    }

    @Test
    fun fitBoundsDisengagesFollowMode() {
        camera.followBody(1)
        assertTrue(camera.isFollowing)

        camera.fitBounds(-100.0, -100.0, 100.0, 100.0)
        assertFalse(camera.isFollowing)
        assertEquals(-1, camera.followedBodyIndex)
    }

    @Test
    fun updateFollowDisengagesWhenBodyOutOfBounds() {
        val snapshot = RenderSnapshot(capacity = 2)
        snapshot.count = 1

        camera.followBody(3) // Index 3 does not exist in snapshot of count 1
        camera.updateFollow(snapshot)

        assertFalse(camera.isFollowing)
        assertEquals(-1, camera.followedBodyIndex)
    }
}
