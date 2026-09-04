package com.droidlinkstd.solarsystemautomata.domain.physics

import com.droidlinkstd.solarsystemautomata.data.CelestialBody
import java.util.Arrays

/**
 * Structure of Arrays (SoA) physics state representation.
 *
 * ARCHITECTURAL CONSTRAINTS & INVARIANTS:
 * - Zero-Allocation Hot Loop: Uses flat primitive arrays sized to a static capacity.
 * - 64-bit IEEE 754 precision for kinematics (position, velocity, acceleration) and mass.
 * - Flat layout guarantees cache locality and eliminates object boxing/heap allocations
 *   during simulation ticks.
 */
class PhysicsState(val capacity: Int = DEFAULT_CAPACITY) {

    /** Current number of active celestial bodies in the simulation. */
    var count: Int = 0

    // Kinematics (64-bit IEEE 754 precision)
    val posX: DoubleArray = DoubleArray(capacity)
    val posY: DoubleArray = DoubleArray(capacity)
    val velX: DoubleArray = DoubleArray(capacity)
    val velY: DoubleArray = DoubleArray(capacity)
    val accX: DoubleArray = DoubleArray(capacity)
    val accY: DoubleArray = DoubleArray(capacity)

    // Physical properties
    val mass: DoubleArray = DoubleArray(capacity)
    val radius: FloatArray = FloatArray(capacity)
    val color: IntArray = IntArray(capacity)

    /**
     * Hydrates the flat primitive buffers from a list of domain [CelestialBody] models.
     * This is executed once per scenario load and never inside the hot integration loop.
     */
    fun loadFromDomain(bodies: List<CelestialBody>) {
        val n = Math.min(bodies.size, capacity)
        count = n

        var i = 0
        while (i < n) {
            val body = bodies[i]
            posX[i] = body.positionX
            posY[i] = body.positionY
            velX[i] = body.velocityX
            velY[i] = body.velocityY
            accX[i] = 0.0
            accY[i] = 0.0
            mass[i] = body.mass
            radius[i] = body.radius.toFloat()
            color[i] = body.colorHex.toInt()
            i++
        }

        // Clear remaining slots in the pre-allocated buffers
        if (n < capacity) {
            Arrays.fill(posX, n, capacity, 0.0)
            Arrays.fill(posY, n, capacity, 0.0)
            Arrays.fill(velX, n, capacity, 0.0)
            Arrays.fill(velY, n, capacity, 0.0)
            Arrays.fill(accX, n, capacity, 0.0)
            Arrays.fill(accY, n, capacity, 0.0)
            Arrays.fill(mass, n, capacity, 0.0)
            Arrays.fill(radius, n, capacity, 0f)
            Arrays.fill(color, n, capacity, 0)
        }
    }

    /**
     * Resets the active count and zeroes all internal buffers.
     */
    fun clear() {
        count = 0
        Arrays.fill(posX, 0.0)
        Arrays.fill(posY, 0.0)
        Arrays.fill(velX, 0.0)
        Arrays.fill(velY, 0.0)
        Arrays.fill(accX, 0.0)
        Arrays.fill(accY, 0.0)
        Arrays.fill(mass, 0.0)
        Arrays.fill(radius, 0f)
        Arrays.fill(color, 0)
    }

    /**
     * Calculates total kinetic energy: K = 1/2 * sum(m_i * (vx_i^2 + vy_i^2)).
     * Executes with zero heap allocations.
     */
    fun calculateKineticEnergy(): Double {
        var ke = 0.0
        val n = count
        var i = 0
        while (i < n) {
            val vx = velX[i]
            val vy = velY[i]
            ke += 0.5 * mass[i] * (vx * vx + vy * vy)
            i++
        }
        return ke
    }

    /**
     * Calculates total gravitational potential energy: U = - sum_{i < j} (G * m_i * m_j / sqrt(r_ij^2 + eps^2)).
     * Executes with zero heap allocations.
     */
    fun calculatePotentialEnergy(g: Double, softening: Double = 0.0): Double {
        var pe = 0.0
        val epsSq = softening * softening
        val n = count
        var i = 0
        while (i < n) {
            val xi = posX[i]
            val yi = posY[i]
            val mi = mass[i]
            var j = i + 1
            while (j < n) {
                val dx = posX[j] - xi
                val dy = posY[j] - yi
                val dist = Math.sqrt(dx * dx + dy * dy + epsSq)
                pe -= (g * mi * mass[j]) / dist
                j++
            }
            i++
        }
        return pe
    }

    /**
     * Calculates total mechanical energy (Hamiltonian): H = K + U.
     */
    fun calculateTotalMechanicalEnergy(g: Double, softening: Double = 0.0): Double {
        return calculateKineticEnergy() + calculatePotentialEnergy(g, softening)
    }

    /**
     * Calculates total linear momentum in X direction: Px = sum(m_i * vx_i).
     */
    fun calculateTotalLinearMomentumX(): Double {
        var px = 0.0
        val n = count
        var i = 0
        while (i < n) {
            px += mass[i] * velX[i]
            i++
        }
        return px
    }

    /**
     * Calculates total linear momentum in Y direction: Py = sum(m_i * vy_i).
     */
    fun calculateTotalLinearMomentumY(): Double {
        var py = 0.0
        val n = count
        var i = 0
        while (i < n) {
            py += mass[i] * velY[i]
            i++
        }
        return py
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 256
    }
}
