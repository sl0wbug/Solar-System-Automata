package com.droidlinkstd.solarsystemautomata.ui.rendering

import com.droidlinkstd.solarsystemautomata.domain.physics.RenderSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrbitalTrailBufferTest {

    private lateinit var trailBuffer: OrbitalTrailBuffer

    @Before
    fun setUp() {
        trailBuffer = OrbitalTrailBuffer(maxBodies = 4, trailCapacity = 5)
    }

    @Test
    fun appendsPointsAndCapsAtCapacity() {
        val bodyIndex = 0
        for (i in 0 until 10) {
            trailBuffer.appendPoint(bodyIndex, i.toDouble(), (i * 2).toDouble())
        }

        assertEquals(5, trailBuffer.counts[bodyIndex])

        // Verify head position modulo capacity
        assertEquals(0, trailBuffer.headIndices[bodyIndex])
    }

    @Test
    fun forEachTrailSegmentYieldsChronologicalOrderAfterWrapping() {
        val bodyIndex = 1
        // Append 7 points: 0, 1, 2, 3, 4, 5, 6.
        // With capacity 5, the surviving points should be 2, 3, 4, 5, 6.
        for (i in 0 until 7) {
            trailBuffer.appendPoint(bodyIndex, i.toDouble(), (i * 10).toDouble())
        }

        val observedX = mutableListOf<Double>()
        var lastP = 0f

        trailBuffer.forEachTrailSegment(bodyIndex) { x1, _, x2, _, progress ->
            if (observedX.isEmpty()) {
                observedX.add(x1)
            }
            observedX.add(x2)
            assertTrue("Progress must increase monotonically", progress > lastP)
            lastP = progress
        }

        // We expect 5 points from 4 segments: [2.0, 3.0, 4.0, 5.0, 6.0]
        assertEquals(listOf(2.0, 3.0, 4.0, 5.0, 6.0), observedX)
        assertEquals(1.0f, lastP, 0.001f)
    }

    @Test
    fun sampleRespectsDistanceThreshold() {
        val snapshot = RenderSnapshot(capacity = 2)
        snapshot.count = 1
        snapshot.posX[0] = 0.0
        snapshot.posY[0] = 0.0

        // Initial sample is always recorded
        trailBuffer.sample(snapshot, minDistanceThresholdSq = 100.0)
        assertEquals(1, trailBuffer.counts[0])

        // Move by 5 units (distance squared = 25 < 100) -> should NOT append
        snapshot.posX[0] = 3.0
        snapshot.posY[0] = 4.0
        trailBuffer.sample(snapshot, minDistanceThresholdSq = 100.0)
        assertEquals(1, trailBuffer.counts[0])

        // Move by 10 units (from initial 0,0 to 10,0 -> distSq = 100 >= 100) -> SHOULD append
        snapshot.posX[0] = 10.0
        snapshot.posY[0] = 0.0
        trailBuffer.sample(snapshot, minDistanceThresholdSq = 100.0)
        assertEquals(2, trailBuffer.counts[0])
    }

    @Test
    fun clearResetsAllTracking() {
        trailBuffer.appendPoint(0, 100.0, 200.0)
        trailBuffer.clear()

        assertEquals(0, trailBuffer.counts[0])
        assertEquals(0, trailBuffer.headIndices[0])
        assertEquals(false, trailBuffer.hasSampled[0])
    }
}
