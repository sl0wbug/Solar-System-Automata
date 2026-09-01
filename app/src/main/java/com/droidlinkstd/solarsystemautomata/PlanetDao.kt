package com.droidlinkstd.solarsystemautomata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanetDao {
    @Insert
    suspend fun insertPlanet(planet: Planet)

    @Update
    suspend fun updatePlanet(planet: Planet)

    // Flow automatically emits updates whenever the database changes
    @Query("SELECT * FROM planets_table ORDER BY orbitDistance ASC")
    fun getAllPlanets(): Flow<List<Planet>>

    @Query("DELETE FROM planets_table")
    suspend fun deleteAllPlanets()
}