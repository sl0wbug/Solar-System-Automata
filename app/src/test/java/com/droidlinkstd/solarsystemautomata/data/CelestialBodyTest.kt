package com.droidlinkstd.solarsystemautomata.data

import com.droidlinkstd.solarsystemautomata.Planet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CelestialBodyTest {

    private val earthPlanet = Planet(
        id = 3,
        name = "Earth",
        colorHex = 0xFF4B85C1L,
        eyeSize = 15f,
        eyeDistance = 140f,
        eyeSpeed = 1.0f,
        realSize = 1.00f,
        realDistance = 1.00f,
        realSpeed = 1.0f,
        orbitDistance = 1.00f,
        description = "Our home planet."
    )

    @Test
    fun testPlanetToDomain_preservesDoublePrecision() {
        val domain = earthPlanet.toDomain(useRealScale = true)

        // Precision verification: check that domain fields are 64-bit Double
        assertEquals("Earth", domain.name)
        assertEquals(1.0, domain.positionX, 1e-9)
        assertEquals(0.0, domain.positionY, 1e-9)
        assertEquals(0.0, domain.velocityX, 1e-9)
        assertEquals(1.0, domain.velocityY, 1e-9)
        assertTrue("Mass must be positive Double", domain.mass > 0.0)
        assertEquals(1.0, domain.radius, 1e-9)
    }

    @Test
    fun testPlanetToDomain_orbitalPhase() {
        // 90 degrees (pi / 2 radians)
        val domain = earthPlanet.toDomain(useRealScale = true, initialAngleRad = Math.PI / 2.0)

        // At 90 deg: x = 0, y = distance, vx = -speed, vy = 0
        assertEquals(0.0, domain.positionX, 1e-9)
        assertEquals(1.0, domain.positionY, 1e-9)
        assertEquals(-1.0, domain.velocityX, 1e-9)
        assertEquals(0.0, domain.velocityY, 1e-9)
    }

    @Test
    fun testDomainToEntity_mapping() {
        val domain = earthPlanet.toDomain(useRealScale = true)
        val entity = domain.toEntity()

        assertEquals(earthPlanet.id, entity.id)
        assertEquals(earthPlanet.name, entity.name)
        assertEquals(earthPlanet.colorHex, entity.colorHex)
        assertEquals(earthPlanet.description, entity.description)
    }

    @Test
    fun testRepository_initialPhysicsBodies_withSun() = runBlocking {
        val fakeDao = object : CelestialBodyDao {
            private val list = mutableListOf(earthPlanet)

            override fun getAllBodies() = flowOf(list)
            override suspend fun getAllBodiesOnce() = list
            override suspend fun getBodyById(id: Int) = list.find { it.id == id }
            override suspend fun insertBody(body: Planet): Long {
                list.add(body)
                return body.id.toLong()
            }
            override suspend fun insertBodies(bodies: List<Planet>): List<Long> {
                list.addAll(bodies)
                return bodies.map { it.id.toLong() }
            }
            override suspend fun updateBody(body: Planet) {}
            override suspend fun deleteBody(body: Planet) { list.remove(body) }
            override suspend fun deleteAllBodies() { list.clear() }
        }

        val repository: CelestialBodyRepository = CelestialBodyRepositoryImpl(fakeDao)
        val bodies = repository.getInitialPhysicsBodies(
            presetId = CelestialBodyRepository.PRESET_SOLAR_SYSTEM,
            includeCentralSun = true
        )

        assertEquals(2, bodies.size)
        // First body is Sun
        val sun = bodies[0]
        assertEquals("Sun", sun.name)
        assertEquals(0.0, sun.positionX, 1e-9)
        assertEquals(0.0, sun.positionY, 1e-9)
        assertEquals(0.0, sun.velocityX, 1e-9)
        assertEquals(0.0, sun.velocityY, 1e-9)
        assertEquals(CelestialBodyRepository.SUN_RELATIVE_MASS, sun.mass, 1e-9)

        // Second body is Earth
        val earth = bodies[1]
        assertEquals("Earth", earth.name)
        assertEquals(1.0, earth.positionX, 1e-9)
    }

    @Test
    fun testRepository_flowMapping() = runBlocking {
        val fakeDao = object : CelestialBodyDao {
            override fun getAllBodies() = flowOf(listOf(earthPlanet))
            override suspend fun getAllBodiesOnce() = listOf(earthPlanet)
            override suspend fun getBodyById(id: Int) = earthPlanet
            override suspend fun insertBody(body: Planet) = 1L
            override suspend fun insertBodies(bodies: List<Planet>) = listOf(1L)
            override suspend fun updateBody(body: Planet) {}
            override suspend fun deleteBody(body: Planet) {}
            override suspend fun deleteAllBodies() {}
        }

        val repository = CelestialBodyRepositoryImpl(fakeDao)
        val list = repository.getBodiesFlow().first()
        assertEquals(1, list.size)
        assertEquals("Earth", list[0].name)
    }
}
