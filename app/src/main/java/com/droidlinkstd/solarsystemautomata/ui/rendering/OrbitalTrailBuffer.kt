package com.droidlinkstd.solarsystemautomata.ui.rendering

import com.droidlinkstd.solarsystemautomata.domain.physics.PhysicsState
import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import java.util.Arrays

/**
 * Pre-allocated circular ring buffer for astronomical orbital trails.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Zero Heap Allocations: Flat primitive arrays for coordinates and indices.
 * - 64-bit IEEE 754 precision: Trails store world coordinates ([DoubleArray]) rather than
 *   screen pixels. This prevents distortion or displacement when panning or zooming the camera.
 * - Distance Threshold Filtering: Prevents oversampling and redundant point creation
 *   when bodies are stationary or move minimally between frames.
 * - Chronological Inline Traversal: [forEachTrailSegment] iterates from oldest to newest point
 *   with zero lambda allocations via Kotlin [inline].
 */
class OrbitalTrailBuffer(
    val maxBodies: Int = PhysicsState.DEFAULT_CAPACITY,
    val trailCapacity: Int = DEFAULT_TRAIL_CAPACITY
) {
    companion object {
        const val DEFAULT_TRAIL_CAPACITY: Int = 120
    }

    private val totalSlots: Int = maxBodies * trailCapacity

    // Flat 64-bit world-space position buffers
    val posX: DoubleArray = DoubleArray(totalSlots)
    val posY: DoubleArray = DoubleArray(totalSlots)

    // Per-body ring buffer tracking
    val counts: IntArray = IntArray(maxBodies)
    val headIndices: IntArray = IntArray(maxBodies)
    val lastSampleX: DoubleArray = DoubleArray(maxBodies)
    val lastSampleY: DoubleArray = DoubleArray(maxBodies)
    val hasSampled: BooleanArray = BooleanArray(maxBodies)

    /**
     * Records new body positions from [snapshot] into the ring buffer.
     * Samples are appended only when a body has moved beyond [minDistanceThresholdSq]
     * in world units squared.
     * Zero heap allocation.
     */
    fun sample(snapshot: RenderSnapshot, minDistanceThresholdSq: Double = 0.0) {
        val n = Math.min(snapshot.count, maxBodies)
        var i = 0
        while (i < n) {
            val curX = snapshot.posX[i]
            val curY = snapshot.posY[i]

            if (!hasSampled[i]) {
                appendPoint(i, curX, curY)
                hasSampled[i] = true
            } else {
                val dx = curX - lastSampleX[i]
                val dy = curY - lastSampleY[i]
                val distSq = dx * dx + dy * dy

                if (distSq >= minDistanceThresholdSq) {
                    appendPoint(i, curX, curY)
                }
            }
            i++
        }
    }

    /**
     * Inserts a single world-space point for body [bodyIndex] into its circular buffer slot.
     */
    fun appendPoint(bodyIndex: Int, x: Double, y: Double) {
        if (bodyIndex < 0 || bodyIndex >= maxBodies) return

        val head = headIndices[bodyIndex]
        val slot = bodyIndex * trailCapacity + head

        posX[slot] = x
        posY[slot] = y

        lastSampleX[bodyIndex] = x
        lastSampleY[bodyIndex] = y

        headIndices[bodyIndex] = (head + 1) % trailCapacity
        if (counts[bodyIndex] < trailCapacity) {
            counts[bodyIndex]++
        }
    }

    /**
     * Resets the trail buffer for all bodies.
     */
    fun clear() {
        Arrays.fill(counts, 0)
        Arrays.fill(headIndices, 0)
        Arrays.fill(hasSampled, false)
        Arrays.fill(posX, 0.0)
        Arrays.fill(posY, 0.0)
        Arrays.fill(lastSampleX, 0.0)
        Arrays.fill(lastSampleY, 0.0)
    }

    /**
     * Zero-allocation inline iteration over all consecutive line segments for [bodyIndex],
     * ordered chronologically from oldest to newest point.
     *
     * [progress] ranges from > 0.0f (tail/oldest) to 1.0f (head/newest).
     */
    inline fun forEachTrailSegment(
        bodyIndex: Int,
        action: (x1: Double, y1: Double, x2: Double, y2: Double, progress: Float) -> Unit
    ) {
        if (bodyIndex < 0 || bodyIndex >= maxBodies) return

        val count = counts[bodyIndex]
        if (count < 2) return

        val startIdx = if (count < trailCapacity) {
            0
        } else {
            headIndices[bodyIndex] // Oldest point is at the current write head
        }

        val baseOffset = bodyIndex * trailCapacity
        val totalSegments = count - 1
        val invTotalSegments = 1.0f / totalSegments

        var step = 0
        while (step < totalSegments) {
            val i1 = (startIdx + step) % trailCapacity
            val i2 = (startIdx + step + 1) % trailCapacity

            val offset1 = baseOffset + i1
            val offset2 = baseOffset + i2

            val progress = (step + 1) * invTotalSegments

            action(
                posX[offset1],
                posY[offset1],
                posX[offset2],
                posY[offset2],
                progress
            )
            step++
        }
    }
}
