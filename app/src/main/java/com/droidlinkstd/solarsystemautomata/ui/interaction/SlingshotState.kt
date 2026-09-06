package com.droidlinkstd.solarsystemautomata.ui.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.droidlinkstd.solarsystemautomata.ui.camera.CameraState
import kotlin.math.sqrt

/**
 * Pre-configured physical presets for user-spawned celestial bodies.
 *
 * Mass scaling relative to Earth (Earth = 1.0, Sun = ~333,000).
 */
enum class SpawnPreset(
    val displayName: String,
    val mass: Double,
    val radius: Float,
    val colorHex: Int,
    val defaultName: String
) {
    ASTEROID(
        displayName = "Asteroid",
        mass = 0.005,
        radius = 4.0f,
        colorHex = 0xFFA0AEC0.toInt(),
        defaultName = "Asteroid"
    ),
    PLANET(
        displayName = "Planet",
        mass = 1.0,
        radius = 8.0f,
        colorHex = 0xFF38BDF8.toInt(),
        defaultName = "Planet"
    ),
    GAS_GIANT(
        displayName = "Gas Giant",
        mass = 317.8,
        radius = 20.0f,
        colorHex = 0xFFFBBF24.toInt(),
        defaultName = "Gas Giant"
    ),
    STAR(
        displayName = "Star",
        mass = 10000.0,
        radius = 36.0f,
        colorHex = 0xFFF97316.toInt(),
        defaultName = "Star"
    )
}

/**
 * State container managing the touch-drag slingshot trajectory and body parameters.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Zero Heap Allocations: Velocity calculations and coordinate tracking operate strictly on primitive scalars.
 * - Zoom Invariance: Velocity calculations scale with camera zoom so physical launch velocity
 *   is consistent regardless of zoom level.
 */
class SlingshotState {
    var isActive: Boolean by mutableStateOf(false)
    var isSpawnModeEnabled: Boolean by mutableStateOf(false)

    var originScreenX: Float by mutableFloatStateOf(0f)
    var originScreenY: Float by mutableFloatStateOf(0f)
    var originWorldX: Double by mutableDoubleStateOf(0.0)
    var originWorldY: Double by mutableDoubleStateOf(0.0)

    var currentDragScreenX: Float by mutableFloatStateOf(0f)
    var currentDragScreenY: Float by mutableFloatStateOf(0f)

    var selectedPreset: SpawnPreset by mutableStateOf(SpawnPreset.PLANET)

    /** Sensitivity factor converting world drag displacement into initial velocity */
    var velocitySensitivity: Double = 0.5

    /**
     * Initiates a slingshot launch gesture at screen point ([screenX], [screenY]).
     */
    fun startSlingshot(screenX: Float, screenY: Float, cameraState: CameraState) {
        isActive = true
        originScreenX = screenX
        originScreenY = screenY
        originWorldX = cameraState.screenToWorldX(screenX)
        originWorldY = cameraState.screenToWorldY(screenY)
        currentDragScreenX = screenX
        currentDragScreenY = screenY
    }

    /**
     * Updates the current drag position during touch movement.
     */
    fun updateDrag(screenX: Float, screenY: Float) {
        if (!isActive) return
        currentDragScreenX = screenX
        currentDragScreenY = screenY
    }

    /**
     * Cancels an active slingshot without launching.
     */
    fun cancel() {
        isActive = false
    }

    /**
     * Calculates the world-space launch velocity X.
     * The launch direction is opposite to the drag direction (slingshot pull).
     */
    fun calculateLaunchVelocityX(cameraState: CameraState): Double {
        if (cameraState.zoom <= 0f) return 0.0
        val screenDx = (currentDragScreenX - originScreenX).toDouble()
        val worldDx = screenDx / cameraState.zoom
        return -worldDx * velocitySensitivity
    }

    /**
     * Calculates the world-space launch velocity Y.
     * The launch direction is opposite to the drag direction (slingshot pull).
     */
    fun calculateLaunchVelocityY(cameraState: CameraState): Double {
        if (cameraState.zoom <= 0f) return 0.0
        val screenDy = (currentDragScreenY - originScreenY).toDouble()
        val worldDy = screenDy / cameraState.zoom
        return -worldDy * velocitySensitivity
    }

    /**
     * Calculates the scalar speed magnitude of the launch vector in world units.
     */
    fun calculateLaunchSpeed(cameraState: CameraState): Double {
        val vx = calculateLaunchVelocityX(cameraState)
        val vy = calculateLaunchVelocityY(cameraState)
        return sqrt(vx * vx + vy * vy)
    }
}
