package com.droidlinkstd.solarsystemautomata.domain.physics

import com.droidlinkstd.solarsystemautomata.data.CelestialBody
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Identifiers for supported celestial scenario presets.
 */
enum class ScenarioPresetId {
    SOLAR_SYSTEM,
    FIGURE_EIGHT,
    BINARY_STAR,
    LAGRANGE_POINTS,
    CHAOTIC_THREE_BODY
}

/**
 * Encapsulates a complete celestial scenario preset configuration.
 *
 * ARCHITECTURAL CONSTRAINTS:
 * - High-precision 64-bit coordinates and velocities.
 * - Net linear momentum strictly conserved ($\sum \vec{P} \approx \vec{0}$) to prevent barycenter drift.
 * - Decoupled completely from Android Compose UI.
 */
data class ScenarioPreset(
    val id: ScenarioPresetId,
    val title: String,
    val subtitle: String,
    val description: String,
    val iconEmoji: String,
    val accentColorHex: Long,
    val g: Double = SimulationEngine.DEFAULT_G,
    val softening: Double = SimulationEngine.DEFAULT_SOFTENING,
    val defaultSpeed: Double = 1.0,
    private val bodiesFactory: () -> List<CelestialBody>
) {
    fun createBodies(): List<CelestialBody> = bodiesFactory()
}

/**
 * Factory providing all 5 canonical orbital mechanics scenario presets.
 */
object ScenarioPresets {

    val ALL_PRESETS: List<ScenarioPreset> by lazy {
        listOf(
            SolarSystem,
            FigureEight,
            BinaryStar,
            LagrangePoints,
            ChaoticThreeBody
        )
    }

    fun getById(id: ScenarioPresetId): ScenarioPreset =
        ALL_PRESETS.firstOrNull { it.id == id } ?: SolarSystem

    // ---------------------------------------------------------------------------------------------
    // 1. Solar System (Default)
    // ---------------------------------------------------------------------------------------------
    val SolarSystem = ScenarioPreset(
        id = ScenarioPresetId.SOLAR_SYSTEM,
        title = "Solar System",
        subtitle = "Sun, 8 Planets & 288 Moons",
        description = "Our home planetary system calibrated in AU and Earth masses, featuring all 288 confirmed natural satellites.",
        iconEmoji = "🌌",
        accentColorHex = 0xFF60A5FA, // Sky Blue
        g = 1.0 / 333000.0,
        softening = 0.0001,
        defaultSpeed = 0.5,
        bodiesFactory = { createSolarSystemBodies() }
    )

    private data class MoonSpec(
        val name: String,
        val mass: Double,
        val radius: Double,
        val relativeDistance: Double,
        val colorHex: Long,
        val description: String,
        val isRetrograde: Boolean = false
    )

    private fun moon(
        name: String,
        dist: Double,
        radius: Double = 0.05,
        mass: Double = 0.00001,
        colorHex: Long = 0xFF94A3B8L,
        desc: String = "Natural satellite.",
        isRetrograde: Boolean = false
    ) = MoonSpec(name, mass, radius, dist, colorHex, desc, isRetrograde)

    private data class PlanetSpec(
        val name: String,
        val distance: Double,
        val angleRad: Double,
        val mass: Double,
        val radius: Double,
        val colorHex: Long,
        val description: String,
        val moons: List<MoonSpec> = emptyList()
    )

    private fun buildEarthMoons(): List<MoonSpec> = listOf(
        moon("Moon", 0.0050, 0.27, 0.0123, 0xFFD1D5DBL, "Earth's natural satellite, Luna.")
    )

    private fun buildMartianMoons(): List<MoonSpec> = listOf(
        moon("Phobos", 0.0025, 0.08, 0.00002, 0xFF8C7D70L, "Inner Martian moon Phobos."),
        moon("Deimos", 0.0045, 0.06, 0.00001, 0xFF786C60L, "Outer Martian moon Deimos.")
    )

    private fun buildJovianMoons(): List<MoonSpec> = listOf(
        // 4 Inner regular moons (Amalthea group)
        moon("Metis", 0.0020, 0.06, 0.00002, 0xFFA8A29EL, "Innermost Jovian moon Metis."),
        moon("Adrastea", 0.0024, 0.05, 0.00001, 0xFFA8A29EL, "Inner shepherd Jovian moon Adrastea."),
        moon("Amalthea", 0.0032, 0.12, 0.00035, 0xFFEF4444L, "Reddish inner Jovian moon Amalthea."),
        moon("Thebe", 0.0042, 0.08, 0.00007, 0xFFA8A29EL, "Outer regular inner Jovian moon Thebe."),
        // 4 Galilean Moons
        moon("Io", 0.0070, 0.28, 0.015, 0xFFF59E0BL, "Volcanic Jovian moon Io."),
        moon("Europa", 0.0110, 0.24, 0.008, 0xFFBAE6FDL, "Subsurface ocean Jovian moon Europa."),
        moon("Ganymede", 0.0175, 0.41, 0.025, 0xFF9CA3AFL, "Largest Solar System moon Ganymede."),
        moon("Callisto", 0.0280, 0.38, 0.018, 0xFF6B7280L, "Ancient cratered Jovian moon Callisto."),
        // 1 Themisto
        moon("Themisto", 0.0480, 0.04, 0.00001, 0xFF94A3B8L, "Prograde irregular Jovian moon Themisto."),
        // 7 Himalia group
        moon("Leda", 0.0680, 0.05, 0.00001, 0xFF94A3B8L, "Himalia group Jovian moon Leda."),
        moon("Himalia", 0.0720, 0.14, 0.0007, 0xFFCBD5E1L, "Largest irregular Jovian moon Himalia."),
        moon("Ersa", 0.0740, 0.03, 0.00001, 0xFF94A3B8L, "Himalia group Jovian moon Ersa."),
        moon("Pandia", 0.0760, 0.03, 0.00001, 0xFF94A3B8L, "Himalia group Jovian moon Pandia."),
        moon("Lysithea", 0.0780, 0.06, 0.00001, 0xFF94A3B8L, "Himalia group Jovian moon Lysithea."),
        moon("Elara", 0.0800, 0.10, 0.00015, 0xFF94A3B8L, "Himalia group Jovian moon Elara."),
        moon("Dia", 0.0840, 0.03, 0.00001, 0xFF94A3B8L, "Himalia group Jovian moon Dia."),
        // 2 Carpo group
        moon("Carpo", 0.0980, 0.04, 0.00001, 0xFF94A3B8L, "High-inclination Jovian moon Carpo."),
        moon("Valetudo", 0.1080, 0.03, 0.00001, 0xFF94A3B8L, "Prograde Jovian moon Valetudo crossing retrograde swarms."),
        // 22 Ananke group (retrograde)
        moon("Euporie", 0.1200, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon Euporie.", isRetrograde = true),
        moon("Eupheme", 0.1215, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon Eupheme.", isRetrograde = true),
        moon("S/2003 J 18", 0.1230, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2003 J 18.", isRetrograde = true),
        moon("Orthosie", 0.1245, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon Orthosie.", isRetrograde = true),
        moon("Euanthe", 0.1260, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon Euanthe.", isRetrograde = true),
        moon("Harpalyke", 0.1275, 0.04, 0.00001, 0xFF94A3B8L, "Ananke group moon Harpalyke.", isRetrograde = true),
        moon("Hermippe", 0.1290, 0.04, 0.00001, 0xFF94A3B8L, "Ananke group moon Hermippe.", isRetrograde = true),
        moon("Praxidike", 0.1305, 0.06, 0.00001, 0xFF94A3B8L, "Ananke group moon Praxidike.", isRetrograde = true),
        moon("Thelxinoe", 0.1320, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon Thelxinoe.", isRetrograde = true),
        moon("Helike", 0.1335, 0.04, 0.00001, 0xFF94A3B8L, "Ananke group moon Helike.", isRetrograde = true),
        moon("Iocaste", 0.1350, 0.05, 0.00001, 0xFF94A3B8L, "Ananke group moon Iocaste.", isRetrograde = true),
        moon("Ananke", 0.1365, 0.08, 0.00005, 0xFF94A3B8L, "Ananke group parent moon Ananke.", isRetrograde = true),
        moon("Mneme", 0.1380, 0.03, 0.00001, 0xFF94A3B8L, "Ananke group moon Mneme.", isRetrograde = true),
        moon("S/2010 J 2", 0.1395, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2010 J 2.", isRetrograde = true),
        moon("S/2016 J 1", 0.1410, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2016 J 1.", isRetrograde = true),
        moon("S/2017 J 3", 0.1425, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2017 J 3.", isRetrograde = true),
        moon("S/2017 J 7", 0.1440, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2017 J 7.", isRetrograde = true),
        moon("S/2017 J 9", 0.1455, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2017 J 9.", isRetrograde = true),
        moon("S/2019 J 1", 0.1470, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2019 J 1.", isRetrograde = true),
        moon("S/2021 J 1", 0.1485, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2021 J 1.", isRetrograde = true),
        moon("S/2021 J 2", 0.1500, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2021 J 2.", isRetrograde = true),
        moon("S/2021 J 3", 0.1515, 0.02, 0.00001, 0xFF94A3B8L, "Ananke group moon S/2021 J 3.", isRetrograde = true),
        // 19 Carme group (retrograde)
        moon("Herse", 0.1530, 0.03, 0.00001, 0xFF94A3B8L, "Carme group moon Herse.", isRetrograde = true),
        moon("Pasithee", 0.1542, 0.03, 0.00001, 0xFF94A3B8L, "Carme group moon Pasithee.", isRetrograde = true),
        moon("Chaldene", 0.1554, 0.04, 0.00001, 0xFF94A3B8L, "Carme group moon Chaldene.", isRetrograde = true),
        moon("Arche", 0.1566, 0.03, 0.00001, 0xFF94A3B8L, "Carme group moon Arche.", isRetrograde = true),
        moon("Isonoe", 0.1578, 0.04, 0.00001, 0xFF94A3B8L, "Carme group moon Isonoe.", isRetrograde = true),
        moon("Erinome", 0.1590, 0.04, 0.00001, 0xFF94A3B8L, "Carme group moon Erinome.", isRetrograde = true),
        moon("Kale", 0.1602, 0.03, 0.00001, 0xFF94A3B8L, "Carme group moon Kale.", isRetrograde = true),
        moon("Aitne", 0.1614, 0.04, 0.00001, 0xFF94A3B8L, "Carme group moon Aitne.", isRetrograde = true),
        moon("Taygete", 0.1626, 0.05, 0.00001, 0xFF94A3B8L, "Carme group moon Taygete.", isRetrograde = true),
        moon("Carme", 0.1638, 0.08, 0.00005, 0xFF94A3B8L, "Carme group parent moon Carme.", isRetrograde = true),
        moon("Sponde", 0.1650, 0.03, 0.00001, 0xFF94A3B8L, "Carme group moon Sponde.", isRetrograde = true),
        moon("Kalyke", 0.1662, 0.05, 0.00001, 0xFF94A3B8L, "Carme group moon Kalyke.", isRetrograde = true),
        moon("Eukelade", 0.1674, 0.04, 0.00001, 0xFF94A3B8L, "Carme group moon Eukelade.", isRetrograde = true),
        moon("S/2003 J 19", 0.1686, 0.03, 0.00001, 0xFF94A3B8L, "Carme group moon S/2003 J 19.", isRetrograde = true),
        moon("S/2011 J 2", 0.1698, 0.02, 0.00001, 0xFF94A3B8L, "Carme group moon S/2011 J 2.", isRetrograde = true),
        moon("S/2017 J 2", 0.1710, 0.02, 0.00001, 0xFF94A3B8L, "Carme group moon S/2017 J 2.", isRetrograde = true),
        moon("S/2017 J 5", 0.1722, 0.02, 0.00001, 0xFF94A3B8L, "Carme group moon S/2017 J 5.", isRetrograde = true),
        moon("S/2017 J 8", 0.1734, 0.02, 0.00001, 0xFF94A3B8L, "Carme group moon S/2017 J 8.", isRetrograde = true),
        moon("S/2018 J 1", 0.1746, 0.02, 0.00001, 0xFF94A3B8L, "Carme group moon S/2018 J 1.", isRetrograde = true),
        // 21 Pasiphae group (retrograde)
        moon("Eurydome", 0.1760, 0.03, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Eurydome.", isRetrograde = true),
        moon("Autonoe", 0.1772, 0.04, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Autonoe.", isRetrograde = true),
        moon("Hegemone", 0.1784, 0.03, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Hegemone.", isRetrograde = true),
        moon("Pasiphae", 0.1796, 0.10, 0.00005, 0xFF94A3B8L, "Pasiphae group parent moon Pasiphae.", isRetrograde = true),
        moon("Sinope", 0.1808, 0.07, 0.00002, 0xFF94A3B8L, "Pasiphae group moon Sinope.", isRetrograde = true),
        moon("S/2003 J 23", 0.1820, 0.03, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2003 J 23.", isRetrograde = true),
        moon("S/2003 J 4", 0.1832, 0.03, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2003 J 4.", isRetrograde = true),
        moon("Callirrhoe", 0.1844, 0.07, 0.00002, 0xFF94A3B8L, "Pasiphae group moon Callirrhoe.", isRetrograde = true),
        moon("Megaclite", 0.1856, 0.05, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Megaclite.", isRetrograde = true),
        moon("Aoede", 0.1868, 0.04, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Aoede.", isRetrograde = true),
        moon("Cyllene", 0.1880, 0.03, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Cyllene.", isRetrograde = true),
        moon("Kore", 0.1892, 0.03, 0.00001, 0xFF94A3B8L, "Pasiphae group moon Kore.", isRetrograde = true),
        moon("S/2011 J 1", 0.1904, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2011 J 1.", isRetrograde = true),
        moon("S/2017 J 1", 0.1916, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2017 J 1.", isRetrograde = true),
        moon("S/2017 J 6", 0.1928, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2017 J 6.", isRetrograde = true),
        moon("S/2018 J 2", 0.1940, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2018 J 2.", isRetrograde = true),
        moon("S/2018 J 3", 0.1952, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2018 J 3.", isRetrograde = true),
        moon("S/2018 J 4", 0.1964, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2018 J 4.", isRetrograde = true),
        moon("S/2021 J 4", 0.1976, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2021 J 4.", isRetrograde = true),
        moon("S/2021 J 5", 0.1988, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2021 J 5.", isRetrograde = true),
        moon("S/2021 J 6", 0.2000, 0.02, 0.00001, 0xFF94A3B8L, "Pasiphae group moon S/2021 J 6.", isRetrograde = true),
        // 15 Outer Provisional irregulars (retrograde)
        moon("S/2003 J 2", 0.2020, 0.03, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2003 J 2.", isRetrograde = true),
        moon("S/2003 J 9", 0.2035, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2003 J 9.", isRetrograde = true),
        moon("S/2003 J 10", 0.2050, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2003 J 10.", isRetrograde = true),
        moon("S/2003 J 12", 0.2065, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2003 J 12.", isRetrograde = true),
        moon("S/2003 J 16", 0.2080, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2003 J 16.", isRetrograde = true),
        moon("S/2016 J 3", 0.2095, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2016 J 3.", isRetrograde = true),
        moon("S/2016 J 4", 0.2110, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2016 J 4.", isRetrograde = true),
        moon("S/2021 J 7", 0.2125, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2021 J 7.", isRetrograde = true),
        moon("S/2021 J 8", 0.2140, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2021 J 8.", isRetrograde = true),
        moon("S/2022 J 1", 0.2155, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2022 J 1.", isRetrograde = true),
        moon("S/2022 J 2", 0.2170, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2022 J 2.", isRetrograde = true),
        moon("S/2022 J 3", 0.2185, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2022 J 3.", isRetrograde = true),
        moon("S/2022 J 4", 0.2200, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2022 J 4.", isRetrograde = true),
        moon("S/2022 J 5", 0.2215, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2022 J 5.", isRetrograde = true),
        moon("S/2022 J 6", 0.2230, 0.02, 0.00001, 0xFF94A3B8L, "Provisional Jovian moon S/2022 J 6.", isRetrograde = true)
    )

    private fun buildSaturnianMoons(): List<MoonSpec> = listOf(
        // 15 Ring Shepherds & Inner Regular
        moon("Pan", 0.0016, 0.04, 0.00001, 0xFFCBD5E1L, "Encke gap shepherd moon Pan."),
        moon("Daphnis", 0.0018, 0.03, 0.00001, 0xFFCBD5E1L, "Keeler gap shepherd moon Daphnis."),
        moon("Atlas", 0.0020, 0.05, 0.00001, 0xFFCBD5E1L, "A ring shepherd moon Atlas."),
        moon("Prometheus", 0.0023, 0.08, 0.00002, 0xFFCBD5E1L, "F ring inner shepherd Prometheus."),
        moon("Pandora", 0.0026, 0.07, 0.00002, 0xFFCBD5E1L, "F ring outer shepherd Pandora."),
        moon("Epimetheus", 0.0030, 0.09, 0.00005, 0xFFCBD5E1L, "Co-orbital moon Epimetheus."),
        moon("Janus", 0.0034, 0.11, 0.00010, 0xFFCBD5E1L, "Co-orbital moon Janus."),
        moon("Aegaeon", 0.0038, 0.02, 0.00001, 0xFFCBD5E1L, "G ring moonlet Aegaeon."),
        moon("Methone", 0.0042, 0.03, 0.00001, 0xFFCBD5E1L, "Alkyonide moon Methone."),
        moon("Anthe", 0.0046, 0.02, 0.00001, 0xFFCBD5E1L, "Alkyonide moon Anthe."),
        moon("Pallene", 0.0050, 0.04, 0.00001, 0xFFCBD5E1L, "Alkyonide moon Pallene."),
        moon("Telesto", 0.0078, 0.04, 0.00001, 0xFFCBD5E1L, "Leading Trojan of Tethys."),
        moon("Calypso", 0.0084, 0.04, 0.00001, 0xFFCBD5E1L, "Trailing Trojan of Tethys."),
        moon("Helene", 0.0105, 0.05, 0.00001, 0xFFCBD5E1L, "Leading Trojan of Dione."),
        moon("Polydeuces", 0.0112, 0.03, 0.00001, 0xFFCBD5E1L, "Trailing Trojan of Dione."),
        // 9 Major Regular & Classical Moons
        moon("Mimas", 0.0056, 0.12, 0.00006, 0xFFCBD5E1L, "Cratered Saturnian moon Mimas."),
        moon("Enceladus", 0.0068, 0.15, 0.00020, 0xFFF8FAFCL, "Reflective ice Saturnian moon Enceladus."),
        moon("Tethys", 0.0082, 0.17, 0.00100, 0xFFCBD5E1L, "Icy Saturnian moon Tethys."),
        moon("Dione", 0.0108, 0.18, 0.00180, 0xFFE2E8F0L, "Bright rayed Saturnian moon Dione."),
        moon("Rhea", 0.0145, 0.24, 0.00390, 0xFFCBD5E1L, "Second-largest Saturnian moon Rhea."),
        moon("Titan", 0.0220, 0.40, 0.02250, 0xFFF97316L, "Dense atmosphere giant moon Titan."),
        moon("Hyperion", 0.0270, 0.11, 0.00001, 0xFFCBD5E1L, "Chaotically rotating moon Hyperion."),
        moon("Iapetus", 0.0420, 0.23, 0.00300, 0xFF94A3B8L, "Two-toned Saturnian moon Iapetus."),
        moon("Phoebe", 0.0650, 0.10, 0.00014, 0xFF64748BL, "Captured Kuiper belt moon Phoebe.", isRetrograde = true),
        // 13 Inuit group (prograde)
        moon("Kiviuq", 0.0750, 0.05, 0.00001, 0xFF94A3B8L, "Inuit group moon Kiviuq."),
        moon("Ijiraq", 0.0770, 0.04, 0.00001, 0xFF94A3B8L, "Inuit group moon Ijiraq."),
        moon("Paaliaq", 0.0800, 0.06, 0.00001, 0xFF94A3B8L, "Inuit group moon Paaliaq."),
        moon("Siarnaq", 0.0850, 0.08, 0.00002, 0xFF94A3B8L, "Largest Inuit group moon Siarnaq."),
        moon("Tarqeq", 0.0900, 0.03, 0.00001, 0xFF94A3B8L, "Inuit group moon Tarqeq."),
        moon("S/2004 S 29", 0.0920, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2004 S 29."),
        moon("S/2004 S 31", 0.0940, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2004 S 31."),
        moon("S/2019 S 1", 0.0960, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2019 S 1."),
        moon("S/2019 S 6", 0.0980, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2019 S 6."),
        moon("S/2019 S 14", 0.1000, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2019 S 14."),
        moon("S/2020 S 1", 0.1020, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2020 S 1."),
        moon("S/2020 S 3", 0.1040, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2020 S 3."),
        moon("S/2020 S 5", 0.1060, 0.02, 0.00001, 0xFF94A3B8L, "Inuit group moon S/2020 S 5."),
        // 6 Gallic group (prograde)
        moon("Albiorix", 0.1090, 0.07, 0.00002, 0xFF94A3B8L, "Largest Gallic group moon Albiorix."),
        moon("Bebhionn", 0.1110, 0.03, 0.00001, 0xFF94A3B8L, "Gallic group moon Bebhionn."),
        moon("Erriapus", 0.1130, 0.04, 0.00001, 0xFF94A3B8L, "Gallic group moon Erriapus."),
        moon("Tarvos", 0.1150, 0.05, 0.00001, 0xFF94A3B8L, "Gallic group moon Tarvos."),
        moon("S/2004 S 24", 0.1170, 0.02, 0.00001, 0xFF94A3B8L, "Gallic group moon S/2004 S 24."),
        moon("S/2007 S 8", 0.1190, 0.02, 0.00001, 0xFF94A3B8L, "Gallic group moon S/2007 S 8."),
        // 103 Norse group (retrograde)
        moon("Skathi", 0.1220, 0.04, 0.00001, 0xFF94A3B8L, "Norse group moon Skathi.", isRetrograde = true),
        moon("S/2007 S 2", 0.1230, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon S/2007 S 2.", isRetrograde = true),
        moon("Skoll", 0.1240, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Skoll.", isRetrograde = true),
        moon("Greip", 0.1250, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Greip.", isRetrograde = true),
        moon("Hyrrokkin", 0.1260, 0.04, 0.00001, 0xFF94A3B8L, "Norse group moon Hyrrokkin.", isRetrograde = true),
        moon("Jarnsaxa", 0.1270, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Jarnsaxa.", isRetrograde = true),
        moon("Mundilfari", 0.1280, 0.04, 0.00001, 0xFF94A3B8L, "Norse group moon Mundilfari.", isRetrograde = true),
        moon("Narvi", 0.1290, 0.04, 0.00001, 0xFF94A3B8L, "Norse group moon Narvi.", isRetrograde = true),
        moon("Bergelmir", 0.1300, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Bergelmir.", isRetrograde = true),
        moon("Aegir", 0.1310, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Aegir.", isRetrograde = true),
        moon("Surtur", 0.1320, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Surtur.", isRetrograde = true),
        moon("Kari", 0.1330, 0.04, 0.00001, 0xFF94A3B8L, "Norse group moon Kari.", isRetrograde = true),
        moon("Fenrir", 0.1340, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Fenrir.", isRetrograde = true),
        moon("Ymir", 0.1350, 0.06, 0.00002, 0xFF94A3B8L, "Largest Norse group moon Ymir.", isRetrograde = true),
        moon("Loge", 0.1360, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Loge.", isRetrograde = true),
        moon("Fornjot", 0.1370, 0.04, 0.00001, 0xFF94A3B8L, "Norse group moon Fornjot.", isRetrograde = true),
        moon("Alvaldi", 0.1380, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Alvaldi.", isRetrograde = true),
        moon("Angrboda", 0.1390, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Angrboda.", isRetrograde = true),
        moon("Beli", 0.1400, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Beli.", isRetrograde = true),
        moon("Eggther", 0.1410, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Eggther.", isRetrograde = true),
        moon("Geirrod", 0.1420, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Geirrod.", isRetrograde = true),
        moon("Gerd", 0.1430, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Gerd.", isRetrograde = true),
        moon("Gridr", 0.1440, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Gridr.", isRetrograde = true),
        moon("Gunnlod", 0.1450, 0.03, 0.00001, 0xFF94A3B8L, "Norse group moon Gunnlod.", isRetrograde = true),
        moon("Thiazzi", 0.1460, 0.02, 0.00001, 0xFF94A3B8L, "Norse group moon Thiazzi.", isRetrograde = true),
        moon("S/2004 S 7", 0.1470, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 7.", isRetrograde = true),
        moon("S/2004 S 12", 0.1480, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 12.", isRetrograde = true),
        moon("S/2004 S 13", 0.1490, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 13.", isRetrograde = true),
        moon("S/2004 S 17", 0.1500, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 17.", isRetrograde = true),
        moon("S/2004 S 20", 0.1510, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 20.", isRetrograde = true),
        moon("S/2004 S 21", 0.1520, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 21.", isRetrograde = true),
        moon("S/2004 S 22", 0.1530, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 22.", isRetrograde = true),
        moon("S/2004 S 23", 0.1540, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 23.", isRetrograde = true),
        moon("S/2004 S 25", 0.1550, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 25.", isRetrograde = true),
        moon("S/2004 S 26", 0.1560, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 26.", isRetrograde = true),
        moon("S/2004 S 27", 0.1570, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 27.", isRetrograde = true),
        moon("S/2004 S 28", 0.1580, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 28.", isRetrograde = true),
        moon("S/2004 S 30", 0.1590, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 30.", isRetrograde = true),
        moon("S/2004 S 32", 0.1600, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 32.", isRetrograde = true),
        moon("S/2004 S 33", 0.1610, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 33.", isRetrograde = true),
        moon("S/2004 S 34", 0.1620, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 34.", isRetrograde = true),
        moon("S/2004 S 35", 0.1630, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 35.", isRetrograde = true),
        moon("S/2004 S 36", 0.1640, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 36.", isRetrograde = true),
        moon("S/2004 S 37", 0.1650, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 37.", isRetrograde = true),
        moon("S/2004 S 38", 0.1660, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 38.", isRetrograde = true),
        moon("S/2004 S 39", 0.1670, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 39.", isRetrograde = true),
        moon("S/2005 S 4", 0.1680, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2005 S 4.", isRetrograde = true),
        moon("S/2005 S 5", 0.1690, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2005 S 5.", isRetrograde = true),
        moon("S/2006 S 1", 0.1700, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 1.", isRetrograde = true),
        moon("S/2006 S 3", 0.1710, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 3.", isRetrograde = true),
        moon("S/2006 S 9", 0.1720, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 9.", isRetrograde = true),
        moon("S/2006 S 10", 0.1730, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 10.", isRetrograde = true),
        moon("S/2006 S 11", 0.1740, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 11.", isRetrograde = true),
        moon("S/2006 S 12", 0.1750, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 12.", isRetrograde = true),
        moon("S/2006 S 13", 0.1760, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 13.", isRetrograde = true),
        moon("S/2006 S 14", 0.1770, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 14.", isRetrograde = true),
        moon("S/2006 S 15", 0.1780, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 15.", isRetrograde = true),
        moon("S/2006 S 16", 0.1790, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 16.", isRetrograde = true),
        moon("S/2006 S 17", 0.1800, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 17.", isRetrograde = true),
        moon("S/2006 S 18", 0.1810, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 18.", isRetrograde = true),
        moon("S/2006 S 19", 0.1820, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 19.", isRetrograde = true),
        moon("S/2006 S 20", 0.1830, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2006 S 20.", isRetrograde = true),
        moon("S/2007 S 3", 0.1840, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2007 S 3.", isRetrograde = true),
        moon("S/2007 S 5", 0.1850, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2007 S 5.", isRetrograde = true),
        moon("S/2007 S 6", 0.1860, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2007 S 6.", isRetrograde = true),
        moon("S/2007 S 7", 0.1870, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2007 S 7.", isRetrograde = true),
        moon("S/2007 S 9", 0.1880, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2007 S 9.", isRetrograde = true),
        moon("S/2019 S 2", 0.1890, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 2.", isRetrograde = true),
        moon("S/2019 S 3", 0.1900, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 3.", isRetrograde = true),
        moon("S/2019 S 4", 0.1910, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 4.", isRetrograde = true),
        moon("S/2019 S 5", 0.1920, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 5.", isRetrograde = true),
        moon("S/2019 S 7", 0.1930, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 7.", isRetrograde = true),
        moon("S/2019 S 8", 0.1940, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 8.", isRetrograde = true),
        moon("S/2019 S 9", 0.1950, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 9.", isRetrograde = true),
        moon("S/2019 S 10", 0.1960, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 10.", isRetrograde = true),
        moon("S/2019 S 11", 0.1970, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 11.", isRetrograde = true),
        moon("S/2019 S 12", 0.1980, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 12.", isRetrograde = true),
        moon("S/2019 S 13", 0.1990, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 13.", isRetrograde = true),
        moon("S/2019 S 15", 0.2000, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 15.", isRetrograde = true),
        moon("S/2019 S 16", 0.2010, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 16.", isRetrograde = true),
        moon("S/2019 S 17", 0.2020, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 17.", isRetrograde = true),
        moon("S/2019 S 18", 0.2030, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 18.", isRetrograde = true),
        moon("S/2019 S 19", 0.2040, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 19.", isRetrograde = true),
        moon("S/2019 S 20", 0.2050, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 20.", isRetrograde = true),
        moon("S/2019 S 21", 0.2060, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2019 S 21.", isRetrograde = true),
        moon("S/2020 S 2", 0.2070, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 2.", isRetrograde = true),
        moon("S/2020 S 4", 0.2080, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 4.", isRetrograde = true),
        moon("S/2020 S 6", 0.2090, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 6.", isRetrograde = true),
        moon("S/2020 S 7", 0.2100, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 7.", isRetrograde = true),
        moon("S/2020 S 8", 0.2110, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 8.", isRetrograde = true),
        moon("S/2020 S 9", 0.2120, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 9.", isRetrograde = true),
        moon("S/2020 S 10", 0.2130, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2020 S 10.", isRetrograde = true),
        moon("S/2004 S 40", 0.2140, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 40.", isRetrograde = true),
        moon("S/2004 S 41", 0.2150, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 41.", isRetrograde = true),
        moon("S/2004 S 42", 0.2160, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 42.", isRetrograde = true),
        moon("S/2004 S 43", 0.2170, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 43.", isRetrograde = true),
        moon("S/2004 S 44", 0.2180, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 44.", isRetrograde = true),
        moon("S/2004 S 45", 0.2190, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 45.", isRetrograde = true),
        moon("S/2004 S 46", 0.2200, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 46.", isRetrograde = true),
        moon("S/2004 S 47", 0.2210, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 47.", isRetrograde = true),
        moon("S/2004 S 48", 0.2220, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 48.", isRetrograde = true),
        moon("S/2004 S 49", 0.2230, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 49.", isRetrograde = true),
        moon("S/2004 S 50", 0.2240, 0.02, 0.00001, 0xFF94A3B8L, "Norse moon S/2004 S 50.", isRetrograde = true)
    )

    private fun buildUranianMoons(): List<MoonSpec> = listOf(
        // 13 Inner Regular Moons
        moon("Cordelia", 0.0015, 0.03, 0.00001, 0xFFCBD5E1L, "Innermost Uranian moon Cordelia."),
        moon("Ophelia", 0.0018, 0.03, 0.00001, 0xFFCBD5E1L, "Outer shepherd Uranian moon Ophelia."),
        moon("Bianca", 0.0021, 0.04, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Bianca."),
        moon("Cressida", 0.0024, 0.06, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Cressida."),
        moon("Desdemona", 0.0027, 0.05, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Desdemona."),
        moon("Juliet", 0.0030, 0.07, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Juliet."),
        moon("Portia", 0.0034, 0.10, 0.00002, 0xFFCBD5E1L, "Second largest inner Uranian moon Portia."),
        moon("Rosalind", 0.0038, 0.05, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Rosalind."),
        moon("Cupid", 0.0042, 0.02, 0.00001, 0xFFCBD5E1L, "Small inner Uranian moon Cupid."),
        moon("Belinda", 0.0046, 0.07, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Belinda."),
        moon("Perdita", 0.0050, 0.03, 0.00001, 0xFFCBD5E1L, "Inner regular Uranian moon Perdita."),
        moon("Puck", 0.0056, 0.12, 0.00005, 0xFFCBD5E1L, "Largest inner Uranian moon Puck."),
        moon("Mab", 0.0062, 0.02, 0.00001, 0xFFCBD5E1L, "Inner ring-associated moon Mab."),
        // 5 Major Classical Moons
        moon("Miranda", 0.0080, 0.11, 0.00010, 0xFFCBD5E1L, "Extremely rugged Uranian moon Miranda."),
        moon("Ariel", 0.0120, 0.18, 0.00220, 0xFFE2E8F0L, "Brightest major Uranian moon Ariel."),
        moon("Umbriel", 0.0170, 0.18, 0.00200, 0xFF64748BL, "Dark ancient Uranian moon Umbriel."),
        moon("Titania", 0.0240, 0.24, 0.00570, 0xFFCBD5E1L, "Largest major Uranian moon Titania."),
        moon("Oberon", 0.0330, 0.23, 0.00500, 0xFF94A3B8L, "Outermost major Uranian moon Oberon."),
        // 10 Irregular Moons
        moon("Francisco", 0.0450, 0.02, 0.00001, 0xFF94A3B8L, "Inner irregular Uranian moon Francisco."),
        moon("Caliban", 0.0600, 0.06, 0.00001, 0xFF94A3B8L, "Second largest irregular Uranian moon Caliban.", isRetrograde = true),
        moon("Stephano", 0.0720, 0.03, 0.00001, 0xFF94A3B8L, "Retrograde irregular Uranian moon Stephano.", isRetrograde = true),
        moon("Trinculo", 0.0820, 0.02, 0.00001, 0xFF94A3B8L, "Small irregular Uranian moon Trinculo.", isRetrograde = true),
        moon("Sycorax", 0.0950, 0.11, 0.00004, 0xFF94A3B8L, "Largest irregular Uranian moon Sycorax.", isRetrograde = true),
        moon("Margaret", 0.1080, 0.02, 0.00001, 0xFF94A3B8L, "The only prograde irregular Uranian moon Margaret."),
        moon("Prospero", 0.1200, 0.04, 0.00001, 0xFF94A3B8L, "Retrograde irregular Uranian moon Prospero.", isRetrograde = true),
        moon("Setebos", 0.1320, 0.04, 0.00001, 0xFF94A3B8L, "Retrograde irregular Uranian moon Setebos.", isRetrograde = true),
        moon("Ferdinand", 0.1450, 0.02, 0.00001, 0xFF94A3B8L, "Outermost known Uranian moon Ferdinand.", isRetrograde = true),
        moon("S/2023 U 1", 0.1580, 0.01, 0.00001, 0xFF94A3B8L, "Recently discovered small irregular moon S/2023 U 1.", isRetrograde = true)
    )

    private fun buildNeptunianMoons(): List<MoonSpec> = listOf(
        // 7 Inner Regular Moons
        moon("Naiad", 0.0015, 0.05, 0.00001, 0xFF94A3B8L, "Innermost regular Neptunian moon Naiad."),
        moon("Thalassa", 0.0019, 0.06, 0.00001, 0xFF94A3B8L, "Inner regular Neptunian moon Thalassa."),
        moon("Despina", 0.0024, 0.09, 0.00001, 0xFF94A3B8L, "Inner regular Neptunian moon Despina."),
        moon("Galatea", 0.0030, 0.11, 0.00002, 0xFF94A3B8L, "Adams ring shepherd moon Galatea."),
        moon("Larissa", 0.0038, 0.13, 0.00003, 0xFF94A3B8L, "Cratered inner regular moon Larissa."),
        moon("Hippocamp", 0.0048, 0.02, 0.00001, 0xFF94A3B8L, "Small regular inner moon Hippocamp."),
        moon("Proteus", 0.0062, 0.16, 0.00008, 0xFF94A3B8L, "Largest regular inner moon Proteus."),
        // 1 Major Captured Retrograde Moon
        moon("Triton", 0.0120, 0.21, 0.00350, 0xFFA5B4FCL, "Massive retrograde captured Kuiper world Triton.", isRetrograde = true),
        // 8 Irregular Moons
        moon("Nereid", 0.0350, 0.10, 0.00005, 0xFFCBD5E1L, "Highly eccentric irregular moon Nereid."),
        moon("Halimede", 0.0650, 0.04, 0.00001, 0xFF94A3B8L, "Retrograde irregular moon Halimede.", isRetrograde = true),
        moon("Sao", 0.0950, 0.03, 0.00001, 0xFF94A3B8L, "Prograde irregular moon Sao."),
        moon("Laomedeia", 0.1250, 0.03, 0.00001, 0xFF94A3B8L, "Prograde irregular moon Laomedeia."),
        moon("Psamathe", 0.1650, 0.03, 0.00001, 0xFF94A3B8L, "Distant retrograde irregular moon Psamathe.", isRetrograde = true),
        moon("Neso", 0.2050, 0.04, 0.00001, 0xFF94A3B8L, "Most distant known natural satellite Neso.", isRetrograde = true),
        moon("S/2002 N 5", 0.2400, 0.02, 0.00001, 0xFF94A3B8L, "Outer irregular moon S/2002 N 5.", isRetrograde = true),
        moon("S/2021 N 1", 0.2750, 0.02, 0.00001, 0xFF94A3B8L, "Outer irregular moon S/2021 N 1.")
    )

    private fun createSolarSystemBodies(): List<CelestialBody> {
        val sunMass = 333000.0
        val g = 1.0 / sunMass

        val planetsData = listOf(
            PlanetSpec(
                name = "Mercury",
                distance = 0.39,
                angleRad = 45.0 * Math.PI / 180.0,
                mass = 0.055,
                radius = 0.38,
                colorHex = 0xFFA9A9A9L,
                description = "Mercury planet orbiting at 0.39 AU."
            ),
            PlanetSpec(
                name = "Venus",
                distance = 0.72,
                angleRad = 135.0 * Math.PI / 180.0,
                mass = 0.815,
                radius = 0.95,
                colorHex = 0xFFE0C097L,
                description = "Venus planet orbiting at 0.72 AU."
            ),
            PlanetSpec(
                name = "Earth",
                distance = 1.00,
                angleRad = 215.0 * Math.PI / 180.0,
                mass = 1.000,
                radius = 1.00,
                colorHex = 0xFF4B85C1L,
                description = "Earth planet orbiting at 1.00 AU.",
                moons = buildEarthMoons()
            ),
            PlanetSpec(
                name = "Mars",
                distance = 1.52,
                angleRad = 310.0 * Math.PI / 180.0,
                mass = 0.107,
                radius = 0.53,
                colorHex = 0xFFB06443L,
                description = "Mars planet orbiting at 1.52 AU.",
                moons = buildMartianMoons()
            ),
            PlanetSpec(
                name = "Jupiter",
                distance = 5.20,
                angleRad = 65.0 * Math.PI / 180.0,
                mass = 317.8,
                radius = 11.2,
                colorHex = 0xFFC7B198L,
                description = "Jupiter planet orbiting at 5.20 AU.",
                moons = buildJovianMoons()
            ),
            PlanetSpec(
                name = "Saturn",
                distance = 9.58,
                angleRad = 180.0 * Math.PI / 180.0,
                mass = 95.2,
                radius = 9.45,
                colorHex = 0xFFE2C48DL,
                description = "Saturn planet orbiting at 9.58 AU.",
                moons = buildSaturnianMoons()
            ),
            PlanetSpec(
                name = "Uranus",
                distance = 19.2,
                angleRad = 265.0 * Math.PI / 180.0,
                mass = 14.5,
                radius = 4.00,
                colorHex = 0xFF9FC4D0L,
                description = "Uranus planet orbiting at 19.2 AU.",
                moons = buildUranianMoons()
            ),
            PlanetSpec(
                name = "Neptune",
                distance = 30.0,
                angleRad = 350.0 * Math.PI / 180.0,
                mass = 17.1,
                radius = 3.88,
                colorHex = 0xFF3E60BBL,
                description = "Neptune planet orbiting at 30.0 AU.",
                moons = buildNeptunianMoons()
            )
        )

        val planetBodies = ArrayList<CelestialBody>(300)
        var planetIdCounter = 1
        var moonIdCounter = 101

        for (planet in planetsData) {
            val cosTheta = cos(planet.angleRad)
            val sinTheta = sin(planet.angleRad)
            val planetSpeed = sqrt(g * sunMass / planet.distance)
            val planetPosX = planet.distance * cosTheta
            val planetPosY = planet.distance * sinTheta
            val planetVelX = -planetSpeed * sinTheta
            val planetVelY = planetSpeed * cosTheta

            val planetBody = CelestialBody(
                id = planetIdCounter++,
                name = planet.name,
                mass = planet.mass,
                positionX = planetPosX,
                positionY = planetPosY,
                velocityX = planetVelX,
                velocityY = planetVelY,
                radius = planet.radius,
                colorHex = planet.colorHex,
                description = planet.description
            )
            planetBodies.add(planetBody)

            for ((moonIdx, moon) in planet.moons.withIndex()) {
                val moonAngle = planet.angleRad + (moonIdx * 2.39996323) // Golden angle offset
                val cosAngle = cos(moonAngle)
                val sinAngle = sin(moonAngle)
                val relDist = moon.relativeDistance

                val moonPosX = planetPosX + relDist * cosAngle
                val moonPosY = planetPosY + relDist * sinAngle

                val vRelMag = sqrt(g * planet.mass / relDist) * (if (moon.isRetrograde) -1.0 else 1.0)
                val moonVelX = planetVelX - vRelMag * sinAngle
                val moonVelY = planetVelY + vRelMag * cosAngle

                val moonBody = CelestialBody(
                    id = moonIdCounter++,
                    name = moon.name,
                    mass = moon.mass,
                    positionX = moonPosX,
                    positionY = moonPosY,
                    velocityX = moonVelX,
                    velocityY = moonVelY,
                    radius = moon.radius,
                    colorHex = moon.colorHex,
                    description = moon.description
                )
                planetBodies.add(moonBody)
            }
        }

        // Calculate total linear momentum and center of mass of all orbiting bodies
        var sumPx = 0.0
        var sumPy = 0.0
        var sumMx = 0.0
        var sumMy = 0.0

        for (b in planetBodies) {
            sumPx += b.mass * b.velocityX
            sumPy += b.mass * b.velocityY
            sumMx += b.mass * b.positionX
            sumMy += b.mass * b.positionY
        }

        // Center barycenter at (0, 0) and zero net linear momentum so Sun remains stable and centered
        val sunPosX = -sumMx / sunMass
        val sunPosY = -sumMy / sunMass
        val sunVelX = -sumPx / sunMass
        val sunVelY = -sumPy / sunMass

        val sunBody = CelestialBody(
            id = -1,
            name = "Sun",
            mass = sunMass,
            positionX = sunPosX,
            positionY = sunPosY,
            velocityX = sunVelX,
            velocityY = sunVelY,
            radius = 109.0,
            colorHex = 0xFFFFD700L,
            description = "The central star of our solar system."
        )

        val list = ArrayList<CelestialBody>(planetBodies.size + 1)
        list.add(sunBody)
        list.addAll(planetBodies)
        return list
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Figure-8 Three-Body Choreography
    // ---------------------------------------------------------------------------------------------
    val FigureEight = ScenarioPreset(
        id = ScenarioPresetId.FIGURE_EIGHT,
        title = "Figure-8",
        subtitle = "Stable 3-Body Choreography",
        description = "Moore & Chenciner-Montgomery remarkable equal-mass solution tracing an unbroken figure-8.",
        iconEmoji = "♾️",
        accentColorHex = 0xFF38BDF8, // Cyan
        g = 1.0,
        softening = 0.0001, // Low softening preserves delicate orbital resonance
        defaultSpeed = 1.0,
        bodiesFactory = { createFigureEightBodies() }
    )

    private fun createFigureEightBodies(): List<CelestialBody> {
        val mass = 100.0
        val vScale = sqrt(mass) // Velocity scales with sqrt(M)

        // High-precision Simó (2002) initial conditions
        val x1 = 0.97000436
        val y1 = -0.24308753
        val vx1 = 0.46620531 * vScale
        val vy1 = 0.43236573 * vScale

        return listOf(
            CelestialBody(
                id = 1,
                name = "Alpha",
                mass = mass,
                positionX = x1,
                positionY = y1,
                velocityX = vx1,
                velocityY = vy1,
                radius = 10.0,
                colorHex = 0xFF38BDF8L, // Cyan
                description = "Figure-8 choreographic body Alpha."
            ),
            CelestialBody(
                id = 2,
                name = "Beta",
                mass = mass,
                positionX = -x1,
                positionY = -y1,
                velocityX = vx1,
                velocityY = vy1,
                radius = 10.0,
                colorHex = 0xFFF43F5EL, // Rose/Magenta
                description = "Figure-8 choreographic body Beta."
            ),
            CelestialBody(
                id = 3,
                name = "Gamma",
                mass = mass,
                positionX = 0.0,
                positionY = 0.0,
                velocityX = -2.0 * vx1,
                velocityY = -2.0 * vy1,
                radius = 10.0,
                colorHex = 0xFFFBBF24L, // Amber/Gold
                description = "Figure-8 choreographic body Gamma."
            )
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 3. Binary Star System with Circumbinary Planet
    // ---------------------------------------------------------------------------------------------
    val BinaryStar = ScenarioPreset(
        id = ScenarioPresetId.BINARY_STAR,
        title = "Binary Star",
        subtitle = "Twin Stars & Circumbinary Planet",
        description = "Two massive suns in mutual Keplerian orbit orbited by a stable distant Tatooine-like world.",
        iconEmoji = "🪐",
        accentColorHex = 0xFFF59E0B, // Amber
        g = 1.0,
        softening = 0.001,
        defaultSpeed = 1.0,
        bodiesFactory = { createBinaryStarBodies() }
    )

    private fun createBinaryStarBodies(): List<CelestialBody> {
        val mStar = 1000.0
        val separation = 4.0 // Distance between stars = 4 AU
        val rStar = separation / 2.0 // 2 AU from barycenter (0,0)

        // Circular orbit velocity for binary pair: v = sqrt(G * M_star / (2 * separation))
        val vStar = sqrt(1.0 * mStar / (2.0 * separation)) // sqrt(1000 / 8) = 11.180339887

        val totalStarMass = mStar * 2.0
        val rPlanet = 14.0 // Well beyond dynamical stability boundary (~3 * separation)
        val vPlanet = sqrt(1.0 * totalStarMass / rPlanet) // sqrt(2000 / 14) = 11.952286

        return listOf(
            CelestialBody(
                id = 1,
                name = "Sol-Alpha",
                mass = mStar,
                positionX = -rStar,
                positionY = 0.0,
                velocityX = 0.0,
                velocityY = -vStar,
                radius = 28.0,
                colorHex = 0xFFFBBF24L, // Gold Star
                description = "Primary star of the binary pair."
            ),
            CelestialBody(
                id = 2,
                name = "Sol-Beta",
                mass = mStar,
                positionX = rStar,
                positionY = 0.0,
                velocityX = 0.0,
                velocityY = vStar,
                radius = 25.0,
                colorHex = 0xFF38BDF8L, // Cyan Star
                description = "Companion star of the binary pair."
            ),
            CelestialBody(
                id = 3,
                name = "Tatooine",
                mass = 1.0,
                positionX = 0.0,
                positionY = rPlanet,
                velocityX = -vPlanet,
                velocityY = 0.0,
                radius = 3.0,
                colorHex = 0xFF34D399L, // Emerald green terrestrial
                description = "Circumbinary planet in stable distant orbit."
            )
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Lagrange Points ($L_4 / L_5$ Trojan Asteroids)
    // ---------------------------------------------------------------------------------------------
    val LagrangePoints = ScenarioPreset(
        id = ScenarioPresetId.LAGRANGE_POINTS,
        title = "Lagrange Points",
        subtitle = "Stable L4/L5 Trojan Equilibrium",
        description = "Star and orbiting giant with Trojan asteroids trapped in 60° gravitational equilibrium.",
        iconEmoji = "📐",
        accentColorHex = 0xFFA78BFA, // Violet
        g = 1.0,
        softening = 0.001,
        defaultSpeed = 1.0,
        bodiesFactory = { createLagrangeBodies() }
    )

    private fun createLagrangeBodies(): List<CelestialBody> {
        val mSun = 1000.0
        val mPlanet = 10.0
        val totalM = mSun + mPlanet
        val rOrbit = 10.0

        // Circular orbital velocity: v = sqrt(G * (M + m) / R)
        val vOrbit = sqrt(1.0 * totalM / rOrbit) // sqrt(1010 / 10) = 10.0498756

        // Barycentric velocity corrections so net momentum is zero
        val vSunY = -vOrbit * (mPlanet / totalM)
        val vPlanetY = vOrbit * (mSun / totalM)

        // Equilateral triangle positions for L4 (lead) and L5 (trail) at 60 degrees
        val cos60 = cos(Math.PI / 3.0) // 0.5
        val sin60 = sin(Math.PI / 3.0) // 0.8660254

        // L4 coordinates & velocity: 60 degrees ahead
        val l4X = rOrbit * cos60
        val l4Y = rOrbit * sin60
        val l4Vx = -vOrbit * sin60
        val l4Vy = vOrbit * cos60

        // L5 coordinates & velocity: 60 degrees behind
        val l5X = rOrbit * cos60
        val l5Y = -rOrbit * sin60
        val l5Vx = vOrbit * sin60
        val l5Vy = vOrbit * cos60

        return listOf(
            CelestialBody(
                id = 1,
                name = "Centauri Prime",
                mass = mSun,
                positionX = 0.0,
                positionY = 0.0,
                velocityX = 0.0,
                velocityY = vSunY,
                radius = 32.0,
                colorHex = 0xFFF59E0BL, // Golden Sun
                description = "Central primary star."
            ),
            CelestialBody(
                id = 2,
                name = "Jovian Major",
                mass = mPlanet,
                positionX = rOrbit,
                positionY = 0.0,
                velocityX = 0.0,
                velocityY = vPlanetY,
                radius = 12.0,
                colorHex = 0xFFFB923CL, // Orange Gas Giant
                description = "Massive gas giant creating stable Lagrange equilibrium wells."
            ),
            CelestialBody(
                id = 3,
                name = "Trojan (L4)",
                mass = 0.0001,
                positionX = l4X,
                positionY = l4Y,
                velocityX = l4Vx,
                velocityY = l4Vy,
                radius = 2.0,
                colorHex = 0xFF38BDF8L, // Cyan Trojan
                description = "Trojan asteroid librating at stable L4 equilibrium (60° leading)."
            ),
            CelestialBody(
                id = 4,
                name = "Greek (L5)",
                mass = 0.0001,
                positionX = l5X,
                positionY = l5Y,
                velocityX = l5Vx,
                velocityY = l5Vy,
                radius = 2.0,
                colorHex = 0xFFE2E8F0L, // White Greek
                description = "Trojan asteroid librating at stable L5 equilibrium (60° trailing)."
            )
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 5. Chaotic Three-Body System (Pythagorean Problem)
    // ---------------------------------------------------------------------------------------------
    val ChaoticThreeBody = ScenarioPreset(
        id = ScenarioPresetId.CHAOTIC_THREE_BODY,
        title = "Chaotic 3-Body",
        subtitle = "Pythagorean Slingshot & Ejection",
        description = "Burrau (1913) classic 3-4-5 mass triangle initially at rest, showcasing slingshots and ejection.",
        iconEmoji = "💥",
        accentColorHex = 0xFFFB7185, // Rose / Red
        g = 1.0,
        softening = 0.05, // Softening prevents numerical singularity during ultra-close binary flybys
        defaultSpeed = 0.5,
        bodiesFactory = { createChaoticBodies() }
    )

    private fun createChaoticBodies(): List<CelestialBody> {
        // Classic Burrau (1913) / Szebehely & Peters (1967) mass ratios 3 : 4 : 5
        val m1 = 300.0
        val m2 = 400.0
        val m3 = 500.0

        // Vertices of 3-4-5 right triangle placed so Center of Mass is identically (0, 0):
        // Body 1 (m=300) at (1.0, 3.0)
        // Body 2 (m=400) at (-2.0, -1.0)
        // Body 3 (m=500) at (1.0, -1.0)
        // Distances: d(2,3) = 3.0, d(1,3) = 4.0, d(1,2) = 5.0
        // All bodies initially at rest (vx = 0, vy = 0). Net momentum is identically 0.
        return listOf(
            CelestialBody(
                id = 1,
                name = "Body 3 (m=3)",
                mass = m1,
                positionX = 1.0,
                positionY = 3.0,
                velocityX = 0.0,
                velocityY = 0.0,
                radius = 9.0,
                colorHex = 0xFFFB7185L, // Rose
                description = "Mass 300 body at the right-angle vertex."
            ),
            CelestialBody(
                id = 2,
                name = "Body 4 (m=4)",
                mass = m2,
                positionX = -2.0,
                positionY = -1.0,
                velocityX = 0.0,
                velocityY = 0.0,
                radius = 11.0,
                colorHex = 0xFF34D399L, // Emerald
                description = "Mass 400 body on side of length 3."
            ),
            CelestialBody(
                id = 3,
                name = "Body 5 (m=5)",
                mass = m3,
                positionX = 1.0,
                positionY = -1.0,
                velocityX = 0.0,
                velocityY = 0.0,
                radius = 13.0,
                colorHex = 0xFFA78BFAL, // Violet
                description = "Mass 500 body on side of length 4."
            )
        )
    }
}
