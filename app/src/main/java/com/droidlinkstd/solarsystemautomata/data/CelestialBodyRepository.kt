package com.droidlinkstd.solarsystemautomata.data

import com.droidlinkstd.solarsystemautomata.Planet
import com.droidlinkstd.solarsystemautomata.PlanetDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository interface exposing high-precision domain models to the physics module.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - Decoupled completely from Compose UI.
 * - PRECISION: Coordinates (x, y), velocities (vx, vy), and masses are strictly 64-bit IEEE 754 Double.
 */
interface CelestialBodyRepository {
    /**
     * Observes all celestial bodies as high-precision [CelestialBody] domain models.
     */
    fun getBodiesFlow(useRealScale: Boolean = true): Flow<List<CelestialBody>>

    /**
     * Observes celestial bodies associated with a specific system or preset ID.
     */
    fun getBodiesByPresetFlow(presetId: String, useRealScale: Boolean = true): Flow<List<CelestialBody>>

    /**
     * Suspended one-shot fetch for initializing physics simulation state.
     * Optionally includes the central star (Sun) at the origin.
     */
    suspend fun getInitialPhysicsBodies(
        presetId: String = PRESET_SOLAR_SYSTEM,
        useRealScale: Boolean = true,
        includeCentralSun: Boolean = true
    ): List<CelestialBody>

    /**
     * Persists or updates a celestial body configuration.
     */
    suspend fun saveBody(body: CelestialBody): Long

    /**
     * Updates an existing celestial body configuration.
     */
    suspend fun updateBody(body: CelestialBody)

    /**
     * Deletes a celestial body configuration.
     */
    suspend fun deleteBody(body: CelestialBody)

    companion object {
        const val PRESET_SOLAR_SYSTEM = "SOLAR_SYSTEM"
        const val SUN_ID = -1
        const val SUN_NAME = "Sun"
        const val SUN_COLOR_HEX = 0xFFFFD700L // Golden Sun

        // Earth relative: Earth mass = 1.0, Sun mass ~ 333,000 Earth masses
        const val SUN_RELATIVE_MASS = 333000.0
        const val SUN_RELATIVE_RADIUS = 109.0
    }
}

/**
 * Default implementation of [CelestialBodyRepository] backed by [CelestialBodyDao].
 */
class CelestialBodyRepositoryImpl(
    private val dao: CelestialBodyDao
) : CelestialBodyRepository {

    constructor(planetDao: PlanetDao) : this(CelestialBodyDao.from(planetDao))

    override fun getBodiesFlow(useRealScale: Boolean): Flow<List<CelestialBody>> {
        return dao.getAllBodies().map { entities ->
            entities.map { it.toDomain(useRealScale = useRealScale) }
        }
    }

    override fun getBodiesByPresetFlow(presetId: String, useRealScale: Boolean): Flow<List<CelestialBody>> {
        return dao.getBodiesByPreset(presetId).map { entities ->
            entities.map { it.toDomain(useRealScale = useRealScale) }
        }
    }

    override suspend fun getInitialPhysicsBodies(
        presetId: String,
        useRealScale: Boolean,
        includeCentralSun: Boolean
    ): List<CelestialBody> {
        val entities = dao.getAllBodiesOnce()
        val bodies = entities.map { it.toDomain(useRealScale = useRealScale) }.toMutableList()

        if (includeCentralSun && bodies.none { it.name.equals(CelestialBodyRepository.SUN_NAME, ignoreCase = true) }) {
            // Central Sun at coordinate origin (0.0, 0.0) with zero velocity
            val sun = CelestialBody(
                id = CelestialBodyRepository.SUN_ID,
                name = CelestialBodyRepository.SUN_NAME,
                mass = CelestialBodyRepository.SUN_RELATIVE_MASS,
                positionX = 0.0,
                positionY = 0.0,
                velocityX = 0.0,
                velocityY = 0.0,
                radius = if (useRealScale) CelestialBodyRepository.SUN_RELATIVE_RADIUS else 50.0,
                colorHex = CelestialBodyRepository.SUN_COLOR_HEX,
                description = "The central star of the Solar System."
            )
            bodies.add(0, sun)
        }

        return bodies
    }

    override suspend fun saveBody(body: CelestialBody): Long {
        return dao.insertBody(body.toEntity())
    }

    override suspend fun updateBody(body: CelestialBody) {
        dao.updateBody(body.toEntity())
    }

    override suspend fun deleteBody(body: CelestialBody) {
        dao.deleteBody(body.toEntity())
    }
}
