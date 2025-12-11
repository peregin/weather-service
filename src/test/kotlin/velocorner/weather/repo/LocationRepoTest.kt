package velocorner.weather.repo

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.testcontainers.containers.PostgreSQLContainer
import velocorner.weather.model.*
import velocorner.weather.repo.DatabaseFactory.transact
import velocorner.weather.util.DockerUtil
import kotlin.test.*
import kotlin.test.Test


internal class LocationRepoTest {
    companion object {
        init {
            System.setProperty("testcontainers.logging", "true")
            val dockerSocket = DockerUtil.detectDockerSocket()
            println("docker socket is: " + dockerSocket);
            System.setProperty("DOCKER_HOST", dockerSocket)
        }

        private const val DB_NAME = "location_test"
        private const val DB_USER = "location"
        private const val DB_PASSWORD = "location"

        private lateinit var postgresContainer: PostgreSQLContainer<*>

        @BeforeClass
        @JvmStatic
        fun setupSpec() {
            // Docker 29.0.0 (which is what recent Docker Desktop releases ship) requires client API ≥ 1.44.
            System.setProperty("api.version", "1.44")
            postgresContainer = PostgreSQLContainer<Nothing>("postgres:16.4").apply {
                withDatabaseName(DB_NAME)
                withUsername(DB_USER)
                withPassword(DB_PASSWORD)
                withUrlParam("loggerLevel", "DEBUG") // Add DEBUG logging for Testcontainers
                start()
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDownSpec() {
            if (::postgresContainer.isInitialized) {
                postgresContainer.stop()
            }
        }
    }

    @Before
    fun setup() {
        val config = ConfigFactory.parseString(
            """
            db.url="${postgresContainer.jdbcUrl}"
            db.user="$DB_USER"
            db.password="$DB_PASSWORD"
        """.trimIndent()
        )
        DatabaseFactory.init(config = config)
        truncateTables()
    }

    private fun truncateTables() = runBlocking {
        transact {
            PostgresqlCurrentWeatherTable.deleteAll()
            PostgresqlForecastWeatherTable.deleteAll()
            LocationTable.deleteAll()
        }
    }

    private val locationRepo = LocationRepoImpl()

    @Test
    fun `should store and retrieve location`() = runBlocking {
        // given
        val location = "Zurich"
        val position = GeoPosition(47.3769, 8.5417)

        // when
        locationRepo.store(location, position)
        val retrieved = locationRepo.getPosition(location)

        // then
        assertNotNull(retrieved)
        assertEquals(position.latitude, retrieved.latitude)
        assertEquals(position.longitude, retrieved.longitude)
    }

    @Test
    fun `should update existing location`() = runBlocking {
        // given
        val location = "Zurich"
        val initialPosition = GeoPosition(47.3769, 8.5417)
        val updatedPosition = GeoPosition(47.3770, 8.5418)

        // when
        locationRepo.store(location, initialPosition)
        locationRepo.store(location, updatedPosition)
        val retrieved = locationRepo.getPosition(location)

        // then
        assertNotNull(retrieved)
        assertEquals(updatedPosition.latitude, retrieved.latitude)
        assertEquals(updatedPosition.longitude, retrieved.longitude)
    }

    @Test
    fun `should handle case-insensitive location retrieval`() = runBlocking {
        // given
        val location = "Zurich"
        val position = GeoPosition(47.3769, 8.5417)

        // when
        locationRepo.store(location, position)

        // then
        assertEquals(position, locationRepo.getPosition("ZURICH"))
        assertEquals(position, locationRepo.getPosition("zurich"))
        assertEquals(position, locationRepo.getPosition("Zurich"))
    }

    @Test
    fun `should return null for non-existent location`() = runBlocking {
        // when
        val result = locationRepo.getPosition("NonExistentCity")

        // then
        assertNull(result)
    }

    @Test
    fun `should suggest locations based on snippet`() = runBlocking {
        // given
        val locations = listOf(
            "Zurich" to GeoPosition(47.3769, 8.5417),
            "Zurich Airport" to GeoPosition(47.4502, 8.5616),
            "Berlin" to GeoPosition(52.5200, 13.4050)
        )

        // when
        locations.forEach { (location, position) ->
            locationRepo.store(location, position)
        }

        // then
        val suggestions = locationRepo.suggestLocations("zur")
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.contains("zurich"))
        assertTrue(suggestions.contains("zurich airport"))
    }

    @Test
    fun `should handle empty snippet for suggestions`() = runBlocking {
        // given
        val location = "Budapest"
        val position = GeoPosition(47.3769, 8.5417)
        locationRepo.store(location, position)

        // when
        val suggestions = locationRepo.suggestLocations("")

        // then
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.contains(location.lowercase()))
    }

    @Test
    fun `should handle case-insensitive suggestions`() = runBlocking {
        // given
        val location = "Zurich"
        val position = GeoPosition(47.3769, 8.5417)
        locationRepo.store(location, position)

        // then
        assertTrue(locationRepo.suggestLocations("ZUR").isNotEmpty())
        assertTrue(locationRepo.suggestLocations("zur").isNotEmpty())
        assertTrue(locationRepo.suggestLocations("Zur").isNotEmpty())
    }

    @Test
    fun `should handle concurrent store operations`() = runBlocking {
        // given
        val location = "Zurich"
        val position1 = GeoPosition(47.3769, 8.5417)
        val position2 = GeoPosition(47.3770, 8.5418)

        // when
        locationRepo.store(location, position1)
        locationRepo.store(location, position2)
        // then
        val retrieved = locationRepo.getPosition(location)
        assertNotNull(retrieved)
        assertTrue(retrieved.latitude in position1.latitude..position2.latitude)
        assertTrue(retrieved.longitude in position1.longitude..position2.longitude)
    }
}
