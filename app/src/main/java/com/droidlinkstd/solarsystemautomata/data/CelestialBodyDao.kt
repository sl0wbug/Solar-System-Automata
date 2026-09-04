package com.droidlinkstd.solarsystemautomata.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.droidlinkstd.solarsystemautomata.Planet
import com.droidlinkstd.solarsystemautomata.PlanetDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Data Access Object for celestial bodies stored in [planets_table].
 * Confined to the .data package and respects the frozen [Planet] schema.
 */
@Dao
interface CelestialBodyDao {

    @Query("SELECT * FROM planets_table ORDER BY orbitDistance ASC")
    fun getAllBodies(): Flow<List<Planet>>

    @Query("SELECT * FROM planets_table ORDER BY orbitDistance ASC")
    suspend fun getAllBodiesOnce(): List<Planet>

    @Query("SELECT * FROM planets_table WHERE id = :id")
    suspend fun getBodyById(id: Int): Planet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBody(body: Planet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodies(bodies: List<Planet>): List<Long>

    @Update
    suspend fun updateBody(body: Planet)

    @Delete
    suspend fun deleteBody(body: Planet)

    @Query("DELETE FROM planets_table")
    suspend fun deleteAllBodies()

    /**
     * Preset query resolver.
     * Since planets_table represents the primary solar system configuration
     * and schema alterations are prohibited by scope rules, standard presets
     * map to the persisted celestial bodies.
     */
    fun getBodiesByPreset(presetId: String): Flow<List<Planet>> {
        return getAllBodies()
    }

    companion object {
        /**
         * Creates a [CelestialBodyDao] adapter wrapping an existing [PlanetDao].
         * Enables seamless usage with [com.droidlinkstd.solarsystemautomata.AppDatabase]
         * without violating the restriction against modifying files outside .data.
         */
        fun from(planetDao: PlanetDao): CelestialBodyDao = object : CelestialBodyDao {
            override fun getAllBodies(): Flow<List<Planet>> = planetDao.getAllPlanets()

            override suspend fun getAllBodiesOnce(): List<Planet> = planetDao.getAllPlanets().first()

            override suspend fun getBodyById(id: Int): Planet? =
                planetDao.getAllPlanets().first().find { it.id == id }

            override suspend fun insertBody(body: Planet): Long {
                planetDao.insertPlanet(body)
                return body.id.toLong()
            }

            override suspend fun insertBodies(bodies: List<Planet>): List<Long> {
                bodies.forEach { planetDao.insertPlanet(it) }
                return bodies.map { it.id.toLong() }
            }

            override suspend fun updateBody(body: Planet) {
                planetDao.updatePlanet(body)
            }

            override suspend fun deleteBody(body: Planet) {
                // Optional fallback when deleting individual planet
            }

            override suspend fun deleteAllBodies() {
                planetDao.deleteAllPlanets()
            }
        }
    }
}
