package com.droidlinkstd.solarsystemautomata.data

import com.droidlinkstd.solarsystemautomata.Planet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Typealias representing the existing Room Entity contract for celestial bodies.
 * Preserves binary and schema compatibility with [Planet] (table: planets_table)
 * in accordance with Scope & Boundary Rule 1.
 */
typealias CelestialBodyEntity = Planet

/**
 * Converts a database [Planet] entity to a high-precision [CelestialBody] domain model
 * ready for the decoupled Velocity Verlet physics simulation.
 *
 * @param useRealScale When true, uses real-scale astronomical parameters (AU, Earth relative speed/size).
 *                     When false, uses eye-pleasing display parameters.
 * @param initialAngleRad Initial orbital phase angle in radians (default: 0.0).
 * @param customMass Optional explicit mass in 64-bit Double. If null, estimated from radius.
 */
fun Planet.toDomain(
    useRealScale: Boolean = true,
    initialAngleRad: Double = 0.0,
    customMass: Double? = null
): CelestialBody {
    val distance: Double = if (useRealScale) realDistance.toDouble() else eyeDistance.toDouble()
    val speed: Double = if (useRealScale) realSpeed.toDouble() else eyeSpeed.toDouble()
    val radius: Double = if (useRealScale) realSize.toDouble() else eyeSize.toDouble()

    val posX: Double = distance * cos(initialAngleRad)
    val posY: Double = distance * sin(initialAngleRad)
    val velX: Double = -speed * sin(initialAngleRad)
    val velY: Double = speed * cos(initialAngleRad)

    // Estimate mass from volume (M = 4/3 * pi * r^3 * rho) or use custom mass.
    // Earth-relative mass scaling: M proportional to r^3
    val mass: Double = customMass ?: (radius * radius * radius).coerceAtLeast(1e-6)

    return CelestialBody(
        id = id,
        name = name,
        mass = mass,
        positionX = posX,
        positionY = posY,
        velocityX = velX,
        velocityY = velY,
        radius = radius,
        colorHex = colorHex,
        description = description
    )
}

/**
 * Converts a [CelestialBody] domain model back into a [Planet] entity for persistence.
 */
fun CelestialBody.toEntity(
    eyeDistanceMultiplier: Float = 50f,
    eyeSpeedMultiplier: Float = 1f
): Planet {
    val distance = kotlin.math.hypot(positionX, positionY).toFloat()
    val speed = kotlin.math.hypot(velocityX, velocityY).toFloat()
    val size = radius.toFloat()

    return Planet(
        id = id,
        name = name,
        colorHex = colorHex,
        eyeSize = size,
        eyeDistance = distance * eyeDistanceMultiplier,
        eyeSpeed = speed * eyeSpeedMultiplier,
        realSize = size,
        realDistance = distance,
        realSpeed = speed,
        orbitDistance = distance,
        description = description
    )
}
