package com.droidlinkstd.solarsystemautomata.ui.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min

/**
 * Manages the astronomical camera viewport state and coordinate projections.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - 64-bit IEEE 754 precision for world-space center coordinates ([centerX], [centerY]).
 * - Zero-allocation coordinate transformations: [worldToScreenX], [worldToScreenY],
 *   [screenToWorldX], and [screenToWorldY] operate entirely on scalar primitives.
 * - Focal-point invariant pinch-to-zoom: coordinates under the gesture centroid stay fixed on screen.
 * - Snapshot state properties trigger ONLY Draw-phase invalidations when read inside [DrawScope].
 */
class CameraState(
    initialCenterX: Double = 0.0,
    initialCenterY: Double = 0.0,
    initialZoom: Float = 1.0f,
    val minZoom: Float = 1e-15f,
    val maxZoom: Float = 1e8f
) {
    var centerX: Double by mutableDoubleStateOf(initialCenterX)
    var centerY: Double by mutableDoubleStateOf(initialCenterY)
    var zoom: Float by mutableFloatStateOf(initialZoom.coerceIn(minZoom, maxZoom))

    var viewportWidth: Float by mutableFloatStateOf(0f)
    var viewportHeight: Float by mutableFloatStateOf(0f)

    /**
     * Updates viewport dimensions on screen size / orientation changes.
     */
    fun updateViewport(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
    }

    /**
     * Projects 64-bit astronomical world coordinate [worldX] to 32-bit screen pixel X.
     * Zero heap allocation.
     */
    fun worldToScreenX(worldX: Double): Float {
        return ((worldX - centerX) * zoom).toFloat() + viewportWidth * 0.5f
    }

    /**
     * Projects 64-bit astronomical world coordinate [worldY] to 32-bit screen pixel Y.
     * Zero heap allocation.
     */
    fun worldToScreenY(worldY: Double): Float {
        return ((worldY - centerY) * zoom).toFloat() + viewportHeight * 0.5f
    }

    /**
     * Inverts 32-bit screen pixel [screenX] back into 64-bit astronomical world coordinate X.
     * Zero heap allocation.
     */
    fun screenToWorldX(screenX: Float): Double {
        return centerX + (screenX - viewportWidth * 0.5f).toDouble() / zoom
    }

    /**
     * Inverts 32-bit screen pixel [screenY] back into 64-bit astronomical world coordinate Y.
     * Zero heap allocation.
     */
    fun screenToWorldY(screenY: Float): Double {
        return centerY + (screenY - viewportHeight * 0.5f).toDouble() / zoom
    }

    /**
     * Pans the camera by screen pixel delta ([dx], [dy]).
     */
    fun panBy(dx: Float, dy: Float) {
        if (zoom <= 0f) return
        centerX -= dx.toDouble() / zoom
        centerY -= dy.toDouble() / zoom
    }

    /**
     * Zooms the camera relative to gesture [centroid], maintaining the world-space
     * point under [centroid] at the exact same screen position.
     */
    fun zoomBy(centroid: Offset, zoomFactor: Float) {
        if (zoomFactor <= 0f || zoomFactor == 1.0f) return

        val oldZoom = zoom
        val newZoom = (oldZoom * zoomFactor).coerceIn(minZoom, maxZoom)
        if (newZoom == oldZoom) return

        // Calculate world coordinate directly under the gesture centroid
        val worldXAtCentroid = screenToWorldX(centroid.x)
        val worldYAtCentroid = screenToWorldY(centroid.y)

        zoom = newZoom

        // Shift center so the same world coordinate remains under the centroid
        centerX = worldXAtCentroid - (centroid.x - viewportWidth * 0.5f).toDouble() / newZoom
        centerY = worldYAtCentroid - (centroid.y - viewportHeight * 0.5f).toDouble() / newZoom
    }

    /**
     * Focuses the camera directly on the specified celestial body world coordinates.
     */
    fun focusOnBody(posX: Double, posY: Double) {
        centerX = posX
        centerY = posY
    }

    /**
     * Adjusts the camera to fit a bounding box in world space with optional padding.
     */
    fun fitBounds(
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
        paddingPx: Float = 64f
    ) {
        val spanX = maxX - minX
        val spanY = maxY - minY
        val worldSpanX = if (spanX <= 0.0) 1.0 else spanX
        val worldSpanY = if (spanY <= 0.0) 1.0 else spanY

        centerX = (minX + maxX) * 0.5
        centerY = (minY + maxY) * 0.5

        val availableWidth = max(viewportWidth - paddingPx * 2f, 100f)
        val availableHeight = max(viewportHeight - paddingPx * 2f, 100f)

        val zoomX = (availableWidth / worldSpanX).toFloat()
        val zoomY = (availableHeight / worldSpanY).toFloat()

        zoom = min(zoomX, zoomY).coerceIn(minZoom, maxZoom)
    }
}
