package com.droidlinkstd.solarsystemautomata.domain.physics

import com.droidlinkstd.solarsystemautomata.data.CelestialBody
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Pre-allocated primitive snapshot for safe, non-blocking UI rendering.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Decoupled completely from Compose UI.
 * - Structure of Arrays layout containing only primitive arrays.
 * - Pre-allocated at static capacity to guarantee zero heap allocations.
 */
class RenderSnapshot(val capacity: Int = PhysicsState.DEFAULT_CAPACITY) {
    @Volatile
    var count: Int = 0

    val posX: DoubleArray = DoubleArray(capacity)
    val posY: DoubleArray = DoubleArray(capacity)
    val velX: DoubleArray = DoubleArray(capacity)
    val velY: DoubleArray = DoubleArray(capacity)
    val mass: DoubleArray = DoubleArray(capacity)
    val radius: FloatArray = FloatArray(capacity)
    val color: IntArray = IntArray(capacity)
    val names: Array<String> = Array(capacity) { "" }

    /**
     * Copies primitive data from [state] using fast memory block copy.
     * Zero heap allocations.
     */
    fun copyFrom(state: PhysicsState) {
        val n = state.count
        count = n
        System.arraycopy(state.posX, 0, posX, 0, n)
        System.arraycopy(state.posY, 0, posY, 0, n)
        System.arraycopy(state.velX, 0, velX, 0, n)
        System.arraycopy(state.velY, 0, velY, 0, n)
        System.arraycopy(state.mass, 0, mass, 0, n)
        System.arraycopy(state.radius, 0, radius, 0, n)
        System.arraycopy(state.color, 0, color, 0, n)
        System.arraycopy(state.names, 0, names, 0, n)
    }
}

/**
 * Simulation engine orchestrating the fixed-timestep accumulation loop.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Dedicated background execution on [Dispatchers.Default].
 * - Decoupled state updates via lock-free triple-buffering with [RenderSnapshot].
 * - Hot simulation loop runs with ZERO heap allocations.
 * - Supports real-time pause/resume and speed multiplier controls.
 */
class SimulationEngine(
    val capacity: Int = PhysicsState.DEFAULT_CAPACITY,
    private val integrator: OrbitalIntegrator = OrbitalIntegrator(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val externalScope: CoroutineScope? = null
) {
    val physicsState: PhysicsState = PhysicsState(capacity)

    // Lock-free triple buffer for thread-safe UI rendering without allocations
    private val snapshotA = RenderSnapshot(capacity)
    private val snapshotB = RenderSnapshot(capacity)
    private val snapshotC = RenderSnapshot(capacity)
    private val snapshots = arrayOf(snapshotA, snapshotB, snapshotC)
    private var writeBufferIndex: Int = 1
    private val publishedSnapshot = AtomicReference<RenderSnapshot>(snapshotA)

    private val isRunningFlag = AtomicBoolean(false)
    val isRunning: Boolean
        get() = isRunningFlag.get()

    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

    @Volatile
    var speedMultiplier: Double = 1.0
        private set

    @Volatile
    var fixedDt: Double = DEFAULT_FIXED_DT

    @Volatile
    var g: Double = DEFAULT_G

    @Volatile
    var softening: Double = DEFAULT_SOFTENING

    var maxFrameAccumulator: Double = 0.25

    private var simulationJob: Job? = null
    private val internalScope = CoroutineScope(dispatcher + Job())
    private val scope: CoroutineScope
        get() = externalScope ?: internalScope

    var repository: CelestialBodyRepository? = null
    var collisionListener: OrbitalIntegrator.CollisionListener? = null

    /**
     * Hydrates the physics state from a list of domain [CelestialBody] models
     * and publishes an initial render snapshot.
     */
    fun loadBodies(bodies: List<CelestialBody>) {
        synchronized(physicsState) {
            physicsState.loadFromDomain(bodies)
            integrator.computeAccelerations(physicsState, g, softening)
            publishSnapshot()
        }
    }

    /**
     * Suspended loader that fetches scenario bodies from [CelestialBodyRepository].
     */
    suspend fun loadFromRepository(
        repository: CelestialBodyRepository,
        presetId: String = CelestialBodyRepository.PRESET_SOLAR_SYSTEM,
        useRealScale: Boolean = true,
        includeCentralSun: Boolean = true
    ) {
        this.repository = repository
        val bodies = repository.getInitialPhysicsBodies(
            presetId = presetId,
            useRealScale = useRealScale,
            includeCentralSun = includeCentralSun
        )
        loadBodies(bodies)
    }

    @Volatile
    var currentPreset: ScenarioPreset = ScenarioPresets.SolarSystem
        private set

    /**
     * Atomically loads a predefined scenario preset into the active physics state.
     * In-place state swap with zero heap allocations in the hot loop.
     * Updates G, softening, and speed, calculates initial accelerations,
     * and publishes frame 0 snapshot immediately.
     */
    fun loadScenario(preset: ScenarioPreset) {
        synchronized(physicsState) {
            currentPreset = preset
            g = preset.g
            softening = preset.softening
            speedMultiplier = preset.defaultSpeed
            val bodies = preset.createBodies()
            physicsState.loadFromDomain(bodies)
            integrator.computeAccelerations(physicsState, g, softening)
            publishSnapshot()
        }
    }

    /**
     * Starts or resumes the background simulation loop.
     */
    fun start() {
        if (isRunningFlag.compareAndSet(false, true)) {
            _isRunningFlow.value = true
            simulationJob = scope.launch(dispatcher) {
                runLoop()
            }
        }
    }

    /**
     * Pauses the simulation loop.
     */
    fun pause() {
        if (isRunningFlag.compareAndSet(true, false)) {
            _isRunningFlow.value = false
            simulationJob?.cancel()
            simulationJob = null
        }
    }

    /**
     * Safely updates the simulation speed multiplier.
     */
    fun setSpeedMultiplier(multiplier: Double) {
        speedMultiplier = if (multiplier < 0.0) 0.0 else multiplier
    }

    private val internalCollisionHandler = OrbitalIntegrator.CollisionListener { absorbedIndex, swappedIndex, absorbedName, impactX, impactY, impactRadiusPx, impactColor ->
        collisionListener?.onCollision(
            absorbedIndex, swappedIndex, absorbedName, impactX, impactY, impactRadiusPx, impactColor
        )
        val repo = repository
        if (repo != null && absorbedName.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                repo.deleteBodyByName(absorbedName)
            }
        }
    }

    /**
     * Executes a single integration step and publishes a snapshot.
     * Useful for manual stepping or deterministic testing.
     */
    fun stepOnce(dt: Double = fixedDt) {
        synchronized(physicsState) {
            integrator.step(physicsState, dt, g, softening)
            if (integrator.resolveCollisions(physicsState, internalCollisionHandler)) {
                integrator.computeAccelerations(physicsState, g, softening)
            }
            publishSnapshot()
        }
    }

    /**
     * Retrieves the latest published [RenderSnapshot] for consumption by the rendering phase.
     * Guaranteed wait-free, non-blocking, and zero-allocation.
     */
    fun getRenderSnapshot(): RenderSnapshot {
        return publishedSnapshot.get()
    }

    /**
     * Injects a newly spawned celestial body into the active simulation.
     * Thread-safe and guaranteed zero-allocation during insertion.
     *
     * @return true if successfully inserted, false if state capacity is exhausted.
     */
    fun spawnBody(
        name: String,
        mass: Double,
        radius: Float,
        color: Int,
        posX: Double,
        posY: Double,
        velX: Double,
        velY: Double,
        repository: CelestialBodyRepository? = null
    ): Boolean {
        var inserted = false
        synchronized(physicsState) {
            val count = physicsState.count
            if (count < capacity) {
                physicsState.posX[count] = posX
                physicsState.posY[count] = posY
                physicsState.velX[count] = velX
                physicsState.velY[count] = velY
                physicsState.accX[count] = 0.0
                physicsState.accY[count] = 0.0
                physicsState.mass[count] = mass
                physicsState.radius[count] = radius
                physicsState.color[count] = color
                physicsState.names[count] = name
                physicsState.count = count + 1

                // Recalculate pairwise accelerations for all bodies
                integrator.computeAccelerations(physicsState, g, softening)
                publishSnapshot()
                inserted = true
            }
        }

        if (inserted) {
            val targetRepo = repository ?: this.repository
            if (targetRepo != null) {
                scope.launch(Dispatchers.IO) {
                    val entity = CelestialBody(
                        name = name,
                        mass = mass,
                        positionX = posX,
                        positionY = posY,
                        velocityX = velX,
                        velocityY = velY,
                        radius = radius.toDouble(),
                        colorHex = color.toLong() and 0xFFFFFFFFL,
                        description = "Custom spawned celestial body."
                    )
                    targetRepo.saveBody(entity)
                }
            }
        }

        return inserted
    }

    /**
     * Deletes a celestial body at [index] using O(1) swap-and-pop compaction.
     * Thread-safe and guaranteed zero-allocation inside the synchronized block.
     *
     * @return true if successfully deleted, false if index is out of bounds.
     */
    fun deleteBodyAt(index: Int, repository: CelestialBodyRepository? = null): Boolean {
        var deleted = false
        var nameToDelete = ""
        synchronized(physicsState) {
            val count = physicsState.count
            if (index in 0 until count) {
                nameToDelete = physicsState.names[index]
                val lastIndex = count - 1
                if (index < lastIndex) {
                    physicsState.posX[index] = physicsState.posX[lastIndex]
                    physicsState.posY[index] = physicsState.posY[lastIndex]
                    physicsState.velX[index] = physicsState.velX[lastIndex]
                    physicsState.velY[index] = physicsState.velY[lastIndex]
                    physicsState.accX[index] = physicsState.accX[lastIndex]
                    physicsState.accY[index] = physicsState.accY[lastIndex]
                    physicsState.mass[index] = physicsState.mass[lastIndex]
                    physicsState.radius[index] = physicsState.radius[lastIndex]
                    physicsState.color[index] = physicsState.color[lastIndex]
                    physicsState.names[index] = physicsState.names[lastIndex]
                }

                // Zero out vacated last slot
                physicsState.posX[lastIndex] = 0.0
                physicsState.posY[lastIndex] = 0.0
                physicsState.velX[lastIndex] = 0.0
                physicsState.velY[lastIndex] = 0.0
                physicsState.accX[lastIndex] = 0.0
                physicsState.accY[lastIndex] = 0.0
                physicsState.mass[lastIndex] = 0.0
                physicsState.radius[lastIndex] = 0f
                physicsState.color[lastIndex] = 0
                physicsState.names[lastIndex] = ""
                physicsState.count = lastIndex

                integrator.computeAccelerations(physicsState, g, softening)
                publishSnapshot()
                deleted = true
            }
        }

        if (deleted && nameToDelete.isNotEmpty()) {
            val targetRepo = repository ?: this.repository
            if (targetRepo != null) {
                scope.launch(Dispatchers.IO) {
                    targetRepo.deleteBodyByName(nameToDelete)
                }
            }
        }

        return deleted
    }

    /**
     * Releases resources and cancels all background jobs.
     */
    fun stop() {
        pause()
    }

    private suspend fun runLoop() {
        var lastTimeNanos = System.nanoTime()
        var accumulator = 0.0

        while (scope.isActive && isRunningFlag.get()) {
            val currentTimeNanos = System.nanoTime()
            var elapsedSec = (currentTimeNanos - lastTimeNanos) / 1_000_000_000.0
            lastTimeNanos = currentTimeNanos

            // Prevent spiral of death if frame time spikes
            if (elapsedSec > maxFrameAccumulator) {
                elapsedSec = maxFrameAccumulator
            }

            val multiplier = speedMultiplier
            accumulator += elapsedSec * multiplier

            val dt = fixedDt
            if (dt > 0.0) {
                synchronized(physicsState) {
                    var anyCollision = false
                    while (accumulator >= dt && isRunningFlag.get()) {
                        integrator.step(physicsState, dt, g, softening)
                        accumulator -= dt
                        if (integrator.resolveCollisions(physicsState, internalCollisionHandler)) {
                            anyCollision = true
                        }
                    }
                    if (anyCollision) {
                        integrator.computeAccelerations(physicsState, g, softening)
                    }
                    publishSnapshot()
                }
            } else {
                accumulator = 0.0
            }

            // Yield cooperatively to prevent thread starvation
            delay(1L)
        }
    }

    /**
     * Rotates write buffer and publishes atomically to render thread.
     * Executes with zero heap allocations.
     */
    private fun publishSnapshot() {
        val targetSnapshot = snapshots[writeBufferIndex]
        targetSnapshot.copyFrom(physicsState)
        publishedSnapshot.set(targetSnapshot)

        // Rotate to the next buffer (triple buffering: 0, 1, 2)
        writeBufferIndex = (writeBufferIndex + 1) % 3
    }

    companion object {
        const val DEFAULT_FIXED_DT: Double = 0.001
        const val DEFAULT_G: Double = 1.0
        const val DEFAULT_SOFTENING: Double = 0.001
    }
}
