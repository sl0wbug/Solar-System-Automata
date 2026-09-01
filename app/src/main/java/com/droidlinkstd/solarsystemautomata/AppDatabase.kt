package com.droidlinkstd.solarsystemautomata

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Planet::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun planetDao(): PlanetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "solar_system_database"
                )
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.planetDao())
                }
            }
        }

        suspend fun populateDatabase(planetDao: PlanetDao) {
            planetDao.deleteAllPlanets()

            val planets = listOf(
                Planet(name = "Mercury", colorHex = 0xFFA9A9A9, eyeSize = 10f, eyeDistance = 50f, eyeSpeed = 4.1f, realSize = 0.38f, realDistance = 0.39f, realSpeed = 1.6f),
                Planet(name = "Venus", colorHex = 0xFFE0C097, eyeSize = 14f, eyeDistance = 90f, eyeSpeed = 1.6f, realSize = 0.95f, realDistance = 0.72f, realSpeed = 1.17f),
                Planet(name = "Earth", colorHex = 0xFF4b85c1, eyeSize = 15f, eyeDistance = 140f, eyeSpeed = 1.0f, realSize = 1.00f, realDistance = 1.00f, realSpeed = 1.0f),
                Planet(name = "Mars", colorHex = 0xFFB06443, eyeSize = 12f, eyeDistance = 190f, eyeSpeed = 0.53f, realSize = 0.53f, realDistance = 1.52f, realSpeed = 0.8f),
                Planet(name = "Jupiter", colorHex = 0xFFC7B198, eyeSize = 35f, eyeDistance = 270f, eyeSpeed = 0.08f, realSize = 11.2f, realDistance = 5.20f, realSpeed = 0.43f),
                Planet(name = "Saturn", colorHex = 0xFFE2C48D, eyeSize = 30f, eyeDistance = 350f, eyeSpeed = 0.03f, realSize = 9.45f, realDistance = 9.58f, realSpeed = 0.32f),
                Planet(name = "Uranus", colorHex = 0xFF9FC4D0, eyeSize = 22f, eyeDistance = 420f, eyeSpeed = 0.01f, realSize = 4.00f, realDistance = 19.2f, realSpeed = 0.23f),
                Planet(name = "Neptune", colorHex = 0xFF3E60BB, eyeSize = 21f, eyeDistance = 490f, eyeSpeed = 0.006f, realSize = 3.88f, realDistance = 30.0f, realSpeed = 0.18f)
            )

            for (planet in planets) {
                planetDao.insertPlanet(planet)
            }
        }
    }
}
