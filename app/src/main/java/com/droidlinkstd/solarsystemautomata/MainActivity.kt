package com.droidlinkstd.solarsystemautomata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.droidlinkstd.solarsystemautomata.data.CelestialBodyRepositoryImpl
import com.droidlinkstd.solarsystemautomata.ui.SimulationScreen
import com.droidlinkstd.solarsystemautomata.ui.theme.SolarSystemAutomataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = CelestialBodyRepositoryImpl(database.planetDao())

        setContent {
            SolarSystemAutomataTheme {
                SimulationScreen(
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}