package com.droidlinkstd.solarsystemautomata.data

/**
 * High-precision domain model representing a celestial body in the simulation.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Decoupled completely from Android Compose UI.
 * - PRECISION: All coordinates (positionX, positionY), velocities (velocityX, velocityY),
 *   mass, and radius are strictly 64-bit IEEE 754 Double. Never downcast to Float
 *   prior to the final rendering projection step.
 */
data class CelestialBody(
    val id: Int = 0,
    val name: String,
    val mass: Double,
    val positionX: Double,
    val positionY: Double,
    val velocityX: Double,
    val velocityY: Double,
    val radius: Double,
    val colorHex: Long,
    val description: String = ""
) {
    /**
     * Returns a copy of this body with updated 64-bit Double position and velocity.
     */
    fun withMotionState(
        newPositionX: Double,
        newPositionY: Double,
        newVelocityX: Double,
        newVelocityY: Double
    ): CelestialBody = copy(
        positionX = newPositionX,
        positionY = newPositionY,
        velocityX = newVelocityX,
        velocityY = newVelocityY
    )
}
