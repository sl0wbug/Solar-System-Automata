package com.droidlinkstd.solarsystemautomata.domain.physics

import com.droidlinkstd.solarsystemautomata.data.CelestialBody
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val radius: FloatArray = FloatArray(capacity)
    val color: IntArray = IntArray(capacity)

    /**
     * Copies primitive data from [state] using fast memory block copy.
     * Zero heap allocations.
     */
    fun copyFrom(state: PhysicsState) {
        val n = state.count
        count = n
        System.arraycopy(state.posX, 0, posX, 0, n)
        System.arraycopy(state.posY, 0, posY, 0, n)
        System.arraycopy(state.radius, 0, radius, 0, n)
        System.arraycopy(state.color, 0, color, 0, n)
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
        val bodies = repository.getInitialPhysicsBodies(
            presetId = presetId,
            useRealScale = useRealScale,
            includeCentralSun = includeCentralSun
        )
        loadBodies(bodies)
    }

    /**
     * Starts or resumes the background simulation loop.
     */
    fun start() {
        if (isRunningFlag.compareAndSet(false, true)) {
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

    /**
     * Executes a single integration step and publishes a snapshot.
     * Useful for manual stepping or deterministic testing.
     */
    fun stepOnce(dt: Double = fixedDt) {
        synchronized(physicsState) {
            integrator.step(physicsState, dt, g, softening)
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
                    while (accumulator >= dt && isRunningFlag.get()) {
                        integrator.step(physicsState, dt, g, softening)
                        accumulator -= dt
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
