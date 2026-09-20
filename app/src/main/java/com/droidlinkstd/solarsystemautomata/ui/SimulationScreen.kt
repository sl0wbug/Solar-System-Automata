package com.droidlinkstd.solarsystemautomata.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidlinkstd.solarsystemautomata.ui.inspector.BodyInspectorCard
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepository
import com.droidlinkstd.solarsystemautomata.domain.physics.ScenarioPreset
import com.droidlinkstd.solarsystemautomata.domain.physics.ScenarioPresets
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
    var currentPreset by remember { mutableStateOf(ScenarioPresets.SolarSystem) }
    var selectedBodyIndex by remember { mutableStateOf<Int?>(null) }
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

    // Helper to auto-fit camera viewport centered directly on the Sun
    val resetCameraAction = {
        val snapshot = simulationEngine.getRenderSnapshot()
        val count = snapshot.count
        if (count > 0) {
            // Locate the Sun (largest mass or index 0)
            var sunIndex = 0
            var maxMass = -1.0
            var i = 0
            while (i < count) {
                if (snapshot.mass[i] > maxMass) {
                    maxMass = snapshot.mass[i]
                    sunIndex = i
                }
                i++
            }
            val sunX = snapshot.posX[sunIndex]
            val sunY = snapshot.posY[sunIndex]

            // Calculate max orbital distance from the Sun across all celestial bodies
            var maxRadius = 1.0
            i = 0
            while (i < count) {
                val dx = snapshot.posX[i] - sunX
                val dy = snapshot.posY[i] - sunY
                val dist = kotlin.math.hypot(dx, dy)
                if (dist > maxRadius) {
                    maxRadius = dist
                }
                i++
            }

            cameraState.fitCenteredOn(sunX, sunY, maxRadius, paddingPx = 80f)
        } else {
            cameraState.centerX = 0.0
            cameraState.centerY = 0.0
        }
    }

    // Helper to switch scenario presets cleanly and reset buffers
    val onSelectPreset: (ScenarioPreset) -> Unit = { preset ->
        currentPreset = preset
        selectedBodyIndex = null
        simulationEngine.loadScenario(preset)
        trailBuffer.clear()
        shockwaveBuffer.clear()
        cameraState.stopFollowing()
        resetCameraAction()
    }

    // Deletes currently selected body using O(1) swap-and-pop compaction
    val onDeleteSelectedBody: () -> Unit = {
        val target = selectedBodyIndex
        if (target != null) {
            val count = simulationEngine.getRenderSnapshot().count
            if (count > 0) {
                val lastIndex = count - 1
                val deleted = simulationEngine.deleteBodyAt(target, repository)
                if (deleted) {
                    trailBuffer.swapAndPop(target, lastIndex)
                    if (cameraState.followedBodyIndex == target) {
                        cameraState.stopFollowing()
                    } else if (cameraState.followedBodyIndex == lastIndex) {
                        cameraState.followBody(target)
                    }
                    selectedBodyIndex = null
                }
            }
        }
    }

    // Hydrate simulation state from scenario preset on initial load
    LaunchedEffect(simulationEngine) {
        simulationEngine.loadScenario(currentPreset)
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
            },
            onSelectBody = { hitIndex ->
                selectedBodyIndex = hitIndex
            },
            selectedBodyIndex = selectedBodyIndex,
            onFirstLayout = resetCameraAction
        )

        // 2. Floating Body Telemetry Inspector Card
        AnimatedVisibility(
            visible = selectedBodyIndex != null && selectedBodyIndex!! in 0 until simulationEngine.getRenderSnapshot().count,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 185.dp)
        ) {
            selectedBodyIndex?.let { index ->
                val snapshot = simulationEngine.getRenderSnapshot()
                if (index in 0 until snapshot.count) {
                    BodyInspectorCard(
                        snapshot = snapshot,
                        bodyIndex = index,
                        isFollowing = cameraState.isFollowing && cameraState.followedBodyIndex == index,
                        onToggleFollow = {
                            if (cameraState.isFollowing && cameraState.followedBodyIndex == index) {
                                cameraState.stopFollowing()
                            } else {
                                cameraState.followBody(index)
                            }
                        },
                        onDeleteBody = onDeleteSelectedBody,
                        onClose = { selectedBodyIndex = null }
                    )
                }
            }
        }

        // 3. Floating interactive controls & telemetry HUD
        SimulationControlsOverlay(
            simulationEngine = simulationEngine,
            cameraState = cameraState,
            fps = currentFps,
            frameTimeMs = currentFrameTimeMs,
            onResetCamera = resetCameraAction,
            selectedPreset = currentPreset,
            onSelectPreset = onSelectPreset
        )
    }
}
