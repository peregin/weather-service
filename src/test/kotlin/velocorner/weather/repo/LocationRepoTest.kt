package velocorner.weather.repo

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import velocorner.weather.model.*
import kotlin.test.*
import kotlin.test.Test


internal class LocationRepoTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupSpec() {
            TestDatabase.start()
        }
    }

    @Before
    fun setup() {
        DatabaseFactory.init(config = TestDatabase.config())
        truncateTables()
    }

    private fun truncateTables() = runBlocking {
        TestDatabase.truncateAllTables()
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
