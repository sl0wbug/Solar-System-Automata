package com.droidlinkstd.solarsystemautomata.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepository
import com.droidlinkstd.solarsystemautomata.domain.physics.SimulationEngine
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import com.droidlinkstd.solarsystemautomata.ui.overlay.SimulationControlsOverlay
import com.droidlinkstd.solarsystemautomata.ui.rendering.OrbitalTrailBuffer
import com.droidlinkstd.solarsystemautomata.ui.rendering.ShockwaveBuffer
import com.droidlinkstd.solarsystemautomata.ui.rendering.SimulationCanvas
import com.droidlinkstd.solarsystemautomata.ui.rendering.StarfieldBuffer
import com.droidlinkstd.solarsystemautomata.ui.interaction.SlingshotState

/**
 * Top-level screen integrating the simulation engine, canvas renderer, camera viewport,
 * and controls overlay.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Lifecycle Awareness: Pauses simulation loop on [Lifecycle.Event.ON_STOP] and resumes on [Lifecycle.Event.ON_START].
 * - Clean Separation: Holds camera state and trail buffers across recompositions.
 * - Viewport Auto-fit: Fits camera to all celestial bodies upon initial scenario load.
 */
@Composable
fun SimulationScreen(
    repository: CelestialBodyRepository,
    modifier: Modifier = Modifier,
    simulationEngine: SimulationEngine = remember { SimulationEngine() },
    cameraState: CameraState = remember { CameraState() },
    trailBuffer: OrbitalTrailBuffer = remember { OrbitalTrailBuffer() },
    starfieldBuffer: StarfieldBuffer = remember { StarfieldBuffer() },
    shockwaveBuffer: ShockwaveBuffer = remember { ShockwaveBuffer() }
) {
    var currentFps by remember { mutableFloatStateOf(60f) }
    var currentFrameTimeMs by remember { mutableFloatStateOf(16.6f) }
    val slingshotState = remember { SlingshotState() }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe app lifecycle to manage simulation thread execution
    DisposableEffect(lifecycleOwner, simulationEngine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> simulationEngine.pause()
                Lifecycle.Event.ON_START -> simulationEngine.start()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            simulationEngine.stop()
        }
    }

    // Helper to auto-fit camera viewport to all bodies in the snapshot
    val resetCameraAction = {
        val snapshot = simulationEngine.getRenderSnapshot()
        val count = snapshot.count
        if (count > 0) {
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE

            var i = 0
            while (i < count) {
                val x = snapshot.posX[i]
                val y = snapshot.posY[i]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                i++
            }

            // Ensure non-zero bounding box
            if (minX == maxX) {
                minX -= 1e10
                maxX += 1e10
            }
            if (minY == maxY) {
                minY -= 1e10
                maxY += 1e10
            }

            cameraState.fitBounds(minX, minY, maxX, maxY, paddingPx = 80f)
        } else {
            cameraState.centerX = 0.0
            cameraState.centerY = 0.0
        }
    }

    // Hydrate simulation state from database repository on initial load
    LaunchedEffect(repository, simulationEngine) {
        simulationEngine.loadFromRepository(
            repository = repository,
            presetId = CelestialBodyRepository.PRESET_SOLAR_SYSTEM
        )
        resetCameraAction()
        simulationEngine.start()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Hardware-accelerated Canvas (Draw-phase only)
        SimulationCanvas(
            simulationEngine = simulationEngine,
            cameraState = cameraState,
            trailBuffer = trailBuffer,
            starfieldBuffer = starfieldBuffer,
            shockwaveBuffer = shockwaveBuffer,
            slingshotState = slingshotState,
            onSpawnBody = { name, mass, radius, color, posX, posY, velX, velY ->
                simulationEngine.spawnBody(
                    name = name,
                    mass = mass,
                    radius = radius,
                    color = color,
                    posX = posX,
                    posY = posY,
                    velX = velX,
                    velY = velY,
                    repository = repository
                )
            },
            onFrameMetrics = { fps, frameTimeMs ->
                currentFps = fps
                currentFrameTimeMs = frameTimeMs
            }
        )

        // 2. Floating interactive controls & telemetry HUD
        SimulationControlsOverlay(
            simulationEngine = simulationEngine,
            cameraState = cameraState,
            fps = currentFps,
            frameTimeMs = currentFrameTimeMs,
            slingshotState = slingshotState,
            onResetCamera = resetCameraAction
        )
    }
}
