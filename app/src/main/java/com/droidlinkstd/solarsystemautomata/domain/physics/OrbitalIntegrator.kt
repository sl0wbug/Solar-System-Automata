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

                accX[i] += dx * fOnI
                accY[i] += dy * fOnI
                accX[j] -= dx * fOnJ
                accY[j] -= dy * fOnJ

                j++
            }
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

                accX[i] += dx * fOnI
                accY[i] += dy * fOnI
                accX[j] -= dx * fOnJ
                accY[j] -= dy * fOnJ

                j++
            }
            i++
        }
    }
}
