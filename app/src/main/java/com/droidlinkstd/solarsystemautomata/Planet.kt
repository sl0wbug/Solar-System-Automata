package com.droidlinkstd.solarsystemautomata

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planets_table")
data class Planet(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val colorHex: Long, // Use ARGB hex representation (e.g. 0xFFFF0000)

    // Eye Pleasing (Not to scale)
    val eyeSize: Float,
    val eyeDistance: Float, // Distance from sun
    val eyeSpeed: Float, // Orbital speed multiplier

    // Real Scale (Relative to Earth)
    val realSize: Float, // Relative size (Earth = 1f)
    val realDistance: Float, // Distance from sun (AU)
    val realSpeed: Float, // Orbital speed (Earth = 1f)
    
    // Using orbitDistance to satisfy PlanetDao ordering
    val orbitDistance: Float = realDistance 
)
