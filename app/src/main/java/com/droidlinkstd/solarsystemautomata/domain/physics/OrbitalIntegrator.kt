package com.droidlinkstd.solarsystemautomata.domain.physics

/**
 * High-performance, zero-allocation symplectic numerical integrator.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Implements Velocity Verlet (second-order, time-reversible, symplectic).
 * - Conserves the system Hamiltonian (total mechanical energy) over long integration periods.
 * - Applies gravitational softening factor (eps) to eliminate numerical singularities when r -> 0.
 * - Exploits Newton's third law (F_ij = -F_ji) to reduce pairwise calculations to N(N-1)/2.
 * - Strict zero heap allocations inside the hot integration loop: no object allocations,
 *   boxing, or iterators.
 */
class OrbitalIntegrator {

    /**
     * Executes a single Velocity Verlet integration step in-place on [state].
     *
     * @param state The flat primitive Structure of Arrays state buffer.
     * @param dt Timestep in seconds.
     * @param g Gravitational constant.
     * @param softening Gravitational softening parameter (eps >= 0).
     */
    fun step(state: PhysicsState, dt: Double, g: Double, softening: Double) {
        val n = state.count
        if (n <= 0) return

        val halfDt = 0.5 * dt
        val halfDtSq = 0.5 * dt * dt
        val epsSq = softening * softening

        val posX = state.posX
        val posY = state.posY
        val velX = state.velX
        val velY = state.velY
        val accX = state.accX
        val accY = state.accY
        val mass = state.mass

        // Step 1: Position update & Step 2: Half-step velocity update
        // r(t + dt) = r(t) + v(t)*dt + 1/2*a(t)*dt^2
        // v(t + dt/2) = v(t) + 1/2*a(t)*dt
        var i = 0
        while (i < n) {
            val ax = accX[i]
            val ay = accY[i]

            posX[i] += velX[i] * dt + ax * halfDtSq
            posY[i] += velY[i] * dt + ay * halfDtSq

            velX[i] += ax * halfDt
            velY[i] += ay * halfDt

            // Reset acceleration buffers for accumulation at r(t + dt)
            accX[i] = 0.0
            accY[i] = 0.0
            i++
        }

        // Step 3: Compute new pairwise gravitational accelerations a(t + dt)
        // Symmetric N(N-1)/2 interaction loop exploiting Newton's third law
        i = 0
        while (i < n) {
            val xi = posX[i]
            val yi = posY[i]
            val mi = mass[i]
            var axi = accX[i]
            var ayi = accY[i]

            var j = i + 1
            while (j < n) {
                val dx = posX[j] - xi
                val dy = posY[j] - yi
                val rSq = dx * dx + dy * dy + epsSq
                val dist = Math.sqrt(rSq)
                val invR3 = 1.0 / (dist * rSq) // 1 / (r^2 + eps^2)^(3/2)

                val fCommon = g * invR3
                val fOnI = fCommon * mass[j]
                val fOnJ = fCommon * mi

                axi += dx * fOnI
                ayi += dy * fOnI
                accX[j] -= dx * fOnJ
                accY[j] -= dy * fOnJ

                j++
            }
            accX[i] = axi
            accY[i] = ayi
            i++
        }

        // Step 4: Final velocity update
        // v(t + dt) = v(t + dt/2) + 1/2*a(t + dt)*dt
        i = 0
        while (i < n) {
            velX[i] += accX[i] * halfDt
            velY[i] += accY[i] * halfDt
            i++
        }
    }

    /**
     * Computes the gravitational accelerations for the current positions in [state]
     * without advancing time, positions, or velocities.
     * Useful for setting up initial a(0) before the first integration step.
     */
    fun computeAccelerations(state: PhysicsState, g: Double, softening: Double) {
        val n = state.count
        if (n <= 0) return

        val epsSq = softening * softening
        val posX = state.posX
        val posY = state.posY
        val accX = state.accX
        val accY = state.accY
        val mass = state.mass

        var i = 0
        while (i < n) {
            accX[i] = 0.0
            accY[i] = 0.0
            i++
        }

        i = 0
        while (i < n) {
            val xi = posX[i]
            val yi = posY[i]
            val mi = mass[i]
            var axi = accX[i]
            var ayi = accY[i]

            var j = i + 1
            while (j < n) {
                val dx = posX[j] - xi
                val dy = posY[j] - yi
                val rSq = dx * dx + dy * dy + epsSq
                val dist = Math.sqrt(rSq)
                val invR3 = 1.0 / (dist * rSq)

                val fCommon = g * invR3
                val fOnI = fCommon * mass[j]
                val fOnJ = fCommon * mi

                axi += dx * fOnI
                ayi += dy * fOnI
                accX[j] -= dx * fOnJ
                accY[j] -= dy * fOnJ

                j++
            }
            accX[i] = axi
            accY[i] = ayi
            i++
        }
    }

    /**
     * Functional callback interface for collision events.
     * Uses primitive scalar parameters to eliminate heap object allocation.
     */
    fun interface CollisionListener {
        fun onCollision(
            absorbedIndex: Int,
            swappedIndex: Int,
            absorbedName: String,
            impactX: Double,
            impactY: Double,
            impactRadiusPx: Float,
            impactColor: Int
        )
    }

    /**
     * Detects pairwise physical contact between celestial bodies and executes
     * perfectly inelastic collisions with zero-allocation Swap-and-Pop compaction.
     *
     * Invariants:
     * - Conservation of Total Mass: M_new = m_w + m_a
     * - Conservation of Linear Momentum: v_new = (m_w * v_w + m_a * v_a) / M_new
     * - Barycentric Position: r_new = (m_w * r_w + m_a * r_a) / M_new
     * - Conservation of Volume: R_new = cbrt(R_w^3 + R_a^3)
     * - Swap-and-Pop: Absorbed body is replaced by body at count - 1 in O(1) time.
     *
     * @return true if at least one collision occurred, false otherwise.
     */
    fun resolveCollisions(
        state: PhysicsState,
        listener: CollisionListener? = null
    ): Boolean {
        var collisionOccurred = false
        var i = 0

        while (i < state.count) {
            var j = i + 1
            while (j < state.count) {
                val dx = state.posX[j] - state.posX[i]
                val dy = state.posY[j] - state.posY[i]
                val distSq = dx * dx + dy * dy

                val collisionRadius = (state.radius[i] + state.radius[j]) * EARTH_RADIUS_TO_AU
                val collisionRadiusSq = collisionRadius * collisionRadius

                if (distSq <= collisionRadiusSq) {
                    collisionOccurred = true

                    // Determine winner (larger mass) and absorbed (smaller mass)
                    val winnerIdx: Int
                    val absorbedIdx: Int
                    if (state.mass[i] >= state.mass[j]) {
                        winnerIdx = i
                        absorbedIdx = j
                    } else {
                        winnerIdx = j
                        absorbedIdx = i
                    }

                    val mw = state.mass[winnerIdx]
                    val ma = state.mass[absorbedIdx]
                    val mNew = mw + ma

                    // Conservation of linear momentum
                    val vxNew = (mw * state.velX[winnerIdx] + ma * state.velX[absorbedIdx]) / mNew
                    val vyNew = (mw * state.velY[winnerIdx] + ma * state.velY[absorbedIdx]) / mNew

                    // Center of mass (barycenter) position
                    val xNew = (mw * state.posX[winnerIdx] + ma * state.posX[absorbedIdx]) / mNew
                    val yNew = (mw * state.posY[winnerIdx] + ma * state.posY[absorbedIdx]) / mNew

                    // Volume conservation: R_new = cbrt(R_w^3 + R_a^3)
                    val rw = state.radius[winnerIdx].toDouble()
                    val ra = state.radius[absorbedIdx].toDouble()
                    val rNew = Math.cbrt(rw * rw * rw + ra * ra * ra).toFloat()

                    val absorbedName = state.names[absorbedIdx]
                    val impactColor = state.color[absorbedIdx]
                    val impactRadiusPx = (state.radius[winnerIdx] + state.radius[absorbedIdx])

                    // Apply merged state to winner
                    state.mass[winnerIdx] = mNew
                    state.velX[winnerIdx] = vxNew
                    state.velY[winnerIdx] = vyNew
                    state.posX[winnerIdx] = xNew
                    state.posY[winnerIdx] = yNew
                    state.radius[winnerIdx] = rNew

                    // Swap-and-Pop compaction: move last active body (count - 1) into absorbedIdx slot
                    val lastIdx = state.count - 1

                    // Notify listener of collision before modifying the slots
                    listener?.onCollision(
                        absorbedIndex = absorbedIdx,
                        swappedIndex = lastIdx,
                        absorbedName = absorbedName,
                        impactX = xNew,
                        impactY = yNew,
                        impactRadiusPx = impactRadiusPx,
                        impactColor = impactColor
                    )

                    if (absorbedIdx != lastIdx) {
                        state.posX[absorbedIdx] = state.posX[lastIdx]
                        state.posY[absorbedIdx] = state.posY[lastIdx]
                        state.velX[absorbedIdx] = state.velX[lastIdx]
                        state.velY[absorbedIdx] = state.velY[lastIdx]
                        state.accX[absorbedIdx] = state.accX[lastIdx]
                        state.accY[absorbedIdx] = state.accY[lastIdx]
                        state.mass[absorbedIdx] = state.mass[lastIdx]
                        state.radius[absorbedIdx] = state.radius[lastIdx]
                        state.color[absorbedIdx] = state.color[lastIdx]
                        state.names[absorbedIdx] = state.names[lastIdx]
                    }

                    // Zero out the vacated last slot
                    state.posX[lastIdx] = 0.0
                    state.posY[lastIdx] = 0.0
                    state.velX[lastIdx] = 0.0
                    state.velY[lastIdx] = 0.0
                    state.accX[lastIdx] = 0.0
                    state.accY[lastIdx] = 0.0
                    state.mass[lastIdx] = 0.0
                    state.radius[lastIdx] = 0f
                    state.color[lastIdx] = 0
                    state.names[lastIdx] = ""

                    state.count--

                    // Adjust loop indices
                    if (absorbedIdx == i) {
                        j = i + 1
                    }
                    // If absorbedIdx == j, j remains the same to check the newly swapped body at slot j
                } else {
                    j++
                }
            }
            i++
        }

        return collisionOccurred
    }

    companion object {
        const val EARTH_RADIUS_TO_AU: Double = 4.25875e-5 // 1 R_earth in AU
    }
}
