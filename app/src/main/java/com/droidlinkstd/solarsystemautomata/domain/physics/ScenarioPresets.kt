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
        subtitle = "Sun & 8 Keplerian Planets",
        description = "Our home planetary system calibrated in Astronomical Units (AU) and Earth masses.",
        iconEmoji = "🌌",
        accentColorHex = 0xFF60A5FA, // Sky Blue
        g = 1.0,
        softening = 0.001,
        defaultSpeed = 1.0,
        bodiesFactory = { createSolarSystemBodies() }
    )

    private fun createSolarSystemBodies(): List<CelestialBody> {
        val sunMass = 333000.0
        val planetsData = listOf(
            Triple("Mercury", 0.39 to 1.6, 0.055 to 0.38 to 0xFFA9A9A9L),
            Triple("Venus", 0.72 to 1.17, 0.815 to 0.95 to 0xFFE0C097L),
            Triple("Earth", 1.00 to 1.00, 1.000 to 1.00 to 0xFF4B85C1L),
            Triple("Mars", 1.52 to 0.80, 0.107 to 0.53 to 0xFFB06443L),
            Triple("Jupiter", 5.20 to 0.43, 317.8 to 11.2 to 0xFFC7B198L),
            Triple("Saturn", 9.58 to 0.32, 95.2 to 9.45 to 0xFFE2C48DL),
            Triple("Uranus", 19.2 to 0.23, 14.5 to 4.00 to 0xFF9FC4D0L),
            Triple("Neptune", 30.0 to 0.18, 17.1 to 3.88 to 0xFF3E60BBL)
        )

        val list = ArrayList<CelestialBody>(9)
        // Central Sun
        list.add(
            CelestialBody(
                id = -1,
                name = "Sun",
                mass = sunMass,
                positionX = 0.0,
                positionY = 0.0,
                velocityX = 0.0,
                velocityY = 0.0,
                radius = 109.0,
                colorHex = 0xFFFFD700L,
                description = "The central star of our solar system."
            )
        )

        var idCounter = 1
        for ((name, orbit, props) in planetsData) {
            val (dist, speed) = orbit
            val (massRadius, color) = props
            val (mass, radius) = massRadius
            list.add(
                CelestialBody(
                    id = idCounter++,
                    name = name,
                    mass = mass,
                    positionX = dist,
                    positionY = 0.0,
                    velocityX = 0.0,
                    velocityY = speed,
                    radius = radius,
                    colorHex = color,
                    description = "$name planet orbiting at ${dist} AU."
                )
            )
        }

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
