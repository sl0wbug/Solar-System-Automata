package com.droidlinkstd.solarsystemautomata.ui.rendering

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.droidlinkstd.solarsystemautomata.domain.physics.SimulationEngine
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import com.droidlinkstd.solarsystemautomata.ui.interaction.HitTester
import com.droidlinkstd.solarsystemautomata.ui.interaction.SlingshotState
import kotlinx.coroutines.isActive

/**
 * Cached native paint objects reused across render frames to prevent heap allocations.
 */
class SimulationPaintCache {
    val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0xFF070A12.toInt() // Deep cosmic dark blue
    }

    val bodyPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val haloPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val trailPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    val labelPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = 0xCCFFFFFF.toInt()
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    val labelShadowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = 0x88000000.toInt()
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    val reticlePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
        color = 0xFF38BDF8.toInt() // Vibrant electric cyan
    }

    val reticleGlowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6.0f
        isAntiAlias = true
        color = 0x4438BDF8.toInt() // Soft cyan glow
    }

    val reticleCornerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        color = 0xFFBAE6FD.toInt() // Light cyan tick accents
    }

    val slingshotLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        isAntiAlias = true
        color = 0x8894A3B8.toInt()
    }

    val slingshotArrowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        color = 0xFFF59E0B.toInt() // Radiant amber trajectory arrow
    }

    val slingshotPreviewPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val slingshotTextPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = 28f
        textAlign = Paint.Align.CENTER
        color = 0xFFFDE68A.toInt() // Light amber
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    val shockwavePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
}

/**
 * Hardware-accelerated Jetpack Compose Canvas renderer.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Strict Draw-Phase Invalidation: Reading [frameTicker] strictly within [DrawScope]
 *   bypasses Compose Recomposition and Layout phases at 60/120 FPS.
 * - Zero Heap Allocations: No object instantiation in the hot draw path.
 * - Flat Primitive Trails: Historical orbital trajectories rendered from [OrbitalTrailBuffer].
 * - Screen-space visibility clamping: Distant astronomical bodies remain visible with subtle halos.
 */
@Composable
fun SimulationCanvas(
    simulationEngine: SimulationEngine,
    cameraState: CameraState,
    modifier: Modifier = Modifier,
    trailBuffer: OrbitalTrailBuffer = remember { OrbitalTrailBuffer() },
    starfieldBuffer: StarfieldBuffer = remember { StarfieldBuffer() },
    shockwaveBuffer: ShockwaveBuffer = remember { ShockwaveBuffer() },
    paintCache: SimulationPaintCache = remember { SimulationPaintCache() },
    slingshotState: SlingshotState = remember { SlingshotState() },
    onSpawnBody: ((name: String, mass: Double, radius: Float, color: Int, posX: Double, posY: Double, velX: Double, velY: Double) -> Unit)? = null,
    onFrameMetrics: ((fps: Float, frameTimeMs: Float) -> Unit)? = null
) {
    // Frame ticker driven by withFrameNanos to synchronize with Android VSYNC
    var frameTicker by remember { mutableLongStateOf(0L) }

    LaunchedEffect(simulationEngine, shockwaveBuffer, cameraState, trailBuffer) {
        simulationEngine.collisionListener = com.droidlinkstd.solarsystemautomata.domain.physics.OrbitalIntegrator.CollisionListener { absorbedIndex, swappedIndex, _, impactX, impactY, impactRadiusPx, impactColor ->
            shockwaveBuffer.triggerShockwave(impactX, impactY, impactRadiusPx, impactColor)
            if (cameraState.isFollowing) {
                if (cameraState.followedBodyIndex == absorbedIndex) {
                    cameraState.stopFollowing()
                } else if (cameraState.followedBodyIndex == swappedIndex) {
                    cameraState.followBody(absorbedIndex)
                }
            }
            trailBuffer.swapAndPop(absorbedIndex, swappedIndex)
        }
    }

    LaunchedEffect(simulationEngine, shockwaveBuffer) {
        var lastNanos = System.nanoTime()
        var frameCount = 0
        var accumulatedTime = 0.0

        while (isActive) {
            androidx.compose.runtime.withFrameNanos { nowNanos ->
                // Synchronize camera center if tracking a body in follow mode
                val snapshot = simulationEngine.getRenderSnapshot()
                cameraState.updateFollow(snapshot)

                frameTicker = nowNanos

                val dt = (nowNanos - lastNanos) * 1e-9
                lastNanos = nowNanos

                shockwaveBuffer.update(dt.toFloat())

                if (onFrameMetrics != null && dt > 0.0) {
                    frameCount++
                    accumulatedTime += dt
                    if (accumulatedTime >= 0.5) {
                        val fps = (frameCount / accumulatedTime).toFloat()
                        val frameMs = ((accumulatedTime / frameCount) * 1000.0).toFloat()
                        onFrameMetrics(fps, frameMs)
                        frameCount = 0
                        accumulatedTime = 0.0
                    }
                }
            }
        }
    }

    val gestureModifier = if (slingshotState.isSpawnModeEnabled) {
        Modifier.pointerInput(slingshotState, cameraState) {
            detectDragGestures(
                onDragStart = { offset ->
                    slingshotState.startSlingshot(offset.x, offset.y, cameraState)
                },
                onDrag = { change, _ ->
                    change.consume()
                    slingshotState.updateDrag(change.position.x, change.position.y)
                },
                onDragEnd = {
                    if (slingshotState.isActive) {
                        val speed = slingshotState.calculateLaunchSpeed(cameraState)
                        if (speed > 1e-4) {
                            val preset = slingshotState.selectedPreset
                            val vx = slingshotState.calculateLaunchVelocityX(cameraState)
                            val vy = slingshotState.calculateLaunchVelocityY(cameraState)
                            if (onSpawnBody != null) {
                                onSpawnBody(
                                    preset.defaultName,
                                    preset.mass,
                                    preset.radius,
                                    preset.colorHex,
                                    slingshotState.originWorldX,
                                    slingshotState.originWorldY,
                                    vx,
                                    vy
                                )
                            } else {
                                simulationEngine.spawnBody(
                                    name = preset.defaultName,
                                    mass = preset.mass,
                                    radius = preset.radius,
                                    color = preset.colorHex,
                                    posX = slingshotState.originWorldX,
                                    posY = slingshotState.originWorldY,
                                    velX = vx,
                                    velY = vy
                                )
                            }
                        }
                        slingshotState.cancel()
                    }
                },
                onDragCancel = {
                    slingshotState.cancel()
                }
            )
        }
    } else {
        Modifier
            .pointerInput(simulationEngine, cameraState) {
                detectTapGestures { offset ->
                    val snapshot = simulationEngine.getRenderSnapshot()
                    val hitIndex = HitTester.findBodyAtScreenOffset(
                        screenX = offset.x,
                        screenY = offset.y,
                        snapshot = snapshot,
                        cameraState = cameraState
                    )
                    if (hitIndex >= 0) {
                        cameraState.followBody(hitIndex)
                    } else {
                        cameraState.stopFollowing()
                    }
                }
            }
            .pointerInput(cameraState) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    cameraState.panBy(pan.x, pan.y)
                    cameraState.zoomBy(centroid, zoom)
                }
            }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                cameraState.updateViewport(size.width.toFloat(), size.height.toFloat())
            }
            .then(gestureModifier)
    ) {
        // Register draw-phase dependency on frameTicker without triggering recomposition
        val _tick = frameTicker

        val nativeCanvas = drawContext.canvas.nativeCanvas
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        // 1. Deep-space background fill
        nativeCanvas.drawRect(0f, 0f, canvasWidth, canvasHeight, paintCache.bgPaint)

        // 2. Parallax starfield
        starfieldBuffer.draw(nativeCanvas, canvasWidth, canvasHeight, cameraState)

        // Fetch latest snapshot published by the background simulation thread
        val snapshot = simulationEngine.getRenderSnapshot()
        val count = snapshot.count
        if (count <= 0) return@Canvas

        // 3. Dynamic distance threshold for trail sampling (2 screen pixels squared)
        val zoomD = cameraState.zoom.toDouble()
        val minDistanceThresholdSq = if (zoomD > 1e-12) 4.0 / (zoomD * zoomD) else 0.0
        trailBuffer.sample(snapshot, minDistanceThresholdSq)

        // 4. Draw orbital trails with progressive alpha fade
        var bodyIndex = 0
        while (bodyIndex < count) {
            val bodyColor = snapshot.color[bodyIndex]
            val r = (bodyColor shr 16) and 0xFF
            val g = (bodyColor shr 8) and 0xFF
            val b = bodyColor and 0xFF

            trailBuffer.forEachTrailSegment(bodyIndex) { x1, y1, x2, y2, progress ->
                val sx1 = cameraState.worldToScreenX(x1)
                val sy1 = cameraState.worldToScreenY(y1)
                val sx2 = cameraState.worldToScreenX(x2)
                val sy2 = cameraState.worldToScreenY(y2)

                // Alpha fades from near transparent (tail) to solid (head)
                val alpha = (progress * 190).toInt().coerceIn(10, 220)
                paintCache.trailPaint.setARGB(alpha, r, g, b)

                nativeCanvas.drawLine(sx1, sy1, sx2, sy2, paintCache.trailPaint)
            }
            bodyIndex++
        }

        // 4.5. Draw active impact shockwaves beneath celestial bodies
        shockwaveBuffer.draw(nativeCanvas, cameraState, paintCache.shockwavePaint, canvasWidth, canvasHeight)

        // 5. Draw celestial bodies (spheres, halos, and names)
        bodyIndex = 0
        while (bodyIndex < count) {
            val wx = snapshot.posX[bodyIndex]
            val wy = snapshot.posY[bodyIndex]
            val sx = cameraState.worldToScreenX(wx)
            val sy = cameraState.worldToScreenY(wy)

            // Cull bodies completely outside the screen viewport (with margin)
            if (sx >= -100f && sx <= canvasWidth + 100f && sy >= -100f && sy <= canvasHeight + 100f) {
                val rawRadiusPx = snapshot.radius[bodyIndex] * cameraState.zoom
                // Clamp screen radius to keep distant bodies visible
                val radiusPx = rawRadiusPx.coerceIn(3.5f, 45f)

                val bodyColor = snapshot.color[bodyIndex]
                val r = (bodyColor shr 16) and 0xFF
                val g = (bodyColor shr 8) and 0xFF
                val b = bodyColor and 0xFF

                // Draw outer atmospheric/gravitational glow
                paintCache.haloPaint.setARGB(45, r, g, b)
                nativeCanvas.drawCircle(sx, sy, radiusPx * 1.6f, paintCache.haloPaint)

                // Draw solid celestial sphere
                paintCache.bodyPaint.color = bodyColor
                nativeCanvas.drawCircle(sx, sy, radiusPx, paintCache.bodyPaint)

                // Draw name label below the body
                val name = snapshot.names[bodyIndex]
                if (name.isNotEmpty()) {
                    val labelY = sy + radiusPx + 22f
                    // Drop shadow for legibility over trails/stars
                    nativeCanvas.drawText(name, sx + 1f, labelY + 1f, paintCache.labelShadowPaint)
                    nativeCanvas.drawText(name, sx, labelY, paintCache.labelPaint)
                }
            }
            bodyIndex++
        }

        // 6. Draw targeting reticle when a celestial body is tracked in follow mode
        if (cameraState.isFollowing && cameraState.followedBodyIndex in 0 until count) {
            val followedIdx = cameraState.followedBodyIndex
            val fx = cameraState.worldToScreenX(snapshot.posX[followedIdx])
            val fy = cameraState.worldToScreenY(snapshot.posY[followedIdx])

            val rawRadiusPx = snapshot.radius[followedIdx] * cameraState.zoom
            val vr = rawRadiusPx.coerceIn(3.5f, 45f)
            val reticleR = vr + 12f

            // Outer glow ring and sharp cyan reticle
            nativeCanvas.drawCircle(fx, fy, reticleR, paintCache.reticleGlowPaint)
            nativeCanvas.drawCircle(fx, fy, reticleR, paintCache.reticlePaint)

            // 4 compass tick marks
            val tickLen = 6f
            nativeCanvas.drawLine(fx - reticleR - tickLen, fy, fx - reticleR + tickLen, fy, paintCache.reticleCornerPaint)
            nativeCanvas.drawLine(fx + reticleR - tickLen, fy, fx + reticleR + tickLen, fy, paintCache.reticleCornerPaint)
            nativeCanvas.drawLine(fx, fy - reticleR - tickLen, fx, fy - reticleR + tickLen, paintCache.reticleCornerPaint)
            nativeCanvas.drawLine(fx, fy + reticleR - tickLen, fx, fy + reticleR + tickLen, paintCache.reticleCornerPaint)
        }

        // 7. Render Slingshot launch trajectory vector when active
        if (slingshotState.isActive) {
            val ox = slingshotState.originScreenX
            val oy = slingshotState.originScreenY
            val dx = slingshotState.currentDragScreenX
            val dy = slingshotState.currentDragScreenY

            val preset = slingshotState.selectedPreset

            // Draw pull back line (from origin to finger)
            nativeCanvas.drawLine(ox, oy, dx, dy, paintCache.slingshotLinePaint)
            nativeCanvas.drawCircle(dx, dy, 8f, paintCache.slingshotLinePaint)

            // Launch direction is opposite of drag
            val pullDx = dx - ox
            val pullDy = dy - oy
            val launchEndX = ox - pullDx
            val launchEndY = oy - pullDy

            // Draw trajectory arrow line
            nativeCanvas.drawLine(ox, oy, launchEndX, launchEndY, paintCache.slingshotArrowPaint)

            // Draw Arrowhead at (launchEndX, launchEndY)
            val pullDist = Math.sqrt((pullDx * pullDx + pullDy * pullDy).toDouble()).toFloat()
            if (pullDist > 12f) {
                val angle = Math.atan2(-pullDy.toDouble(), -pullDx.toDouble())
                val arrowHeadLen = 20.0
                val arrowAngle = Math.PI / 6.0 // 30 degrees

                val x1 = launchEndX - (arrowHeadLen * Math.cos(angle - arrowAngle)).toFloat()
                val y1 = launchEndY - (arrowHeadLen * Math.sin(angle - arrowAngle)).toFloat()
                val x2 = launchEndX - (arrowHeadLen * Math.cos(angle + arrowAngle)).toFloat()
                val y2 = launchEndY - (arrowHeadLen * Math.sin(angle + arrowAngle)).toFloat()

                nativeCanvas.drawLine(launchEndX, launchEndY, x1, y1, paintCache.slingshotArrowPaint)
                nativeCanvas.drawLine(launchEndX, launchEndY, x2, y2, paintCache.slingshotArrowPaint)

                // Draw speed readout
                val speed = slingshotState.calculateLaunchSpeed(cameraState)
                val labelText = "${preset.displayName} • ${String.format("%.2f", speed)} AU/s"
                nativeCanvas.drawText(labelText, launchEndX, launchEndY - 18f, paintCache.slingshotTextPaint)
            }

            // Draw preview circle of the body to be spawned at origin
            paintCache.slingshotPreviewPaint.color = preset.colorHex
            nativeCanvas.drawCircle(ox, oy, (preset.radius * 1.5f).coerceAtLeast(6f), paintCache.slingshotPreviewPaint)
        }
    }
}
