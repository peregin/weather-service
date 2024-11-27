package velocorner.weather.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import velocorner.weather.model.*
import velocorner.weather.repo.LocationRepo
import velocorner.weather.repo.WeatherRepo
import java.time.OffsetDateTime

class WeatherServiceTest {
    private lateinit var weatherService: WeatherService
    private lateinit var mockFeed: MockOpenWeatherFeed
    private lateinit var mockWeatherRepo: MockWeatherRepo
    private lateinit var mockLocationRepo: MockLocationRepo

    @BeforeTest
    fun setup() {
        mockFeed = MockOpenWeatherFeed()
        mockWeatherRepo = MockWeatherRepo()
        mockLocationRepo = MockLocationRepo()
        weatherService = WeatherService(
            feed = mockFeed,
            weatherRepo = mockWeatherRepo,
            locationRepo = mockLocationRepo
        )
    }

    @Test
    fun `should return cached weather when within refresh timeout`() = runBlocking {
        // given
        val location = "Zurich"
        val weatherResponse = createCurrentWeather()
        val weather = requireNotNull(weatherService.convert(location, weatherResponse))
        mockWeatherRepo.setCurrent(location, weather)

        // when
        val result = weatherService.current(location)

        // then
        assertNotNull(result)
        assertEquals(weather, result)
        assertEquals(0, mockFeed.currentCallCount)
    }

    @Test
    fun `should fetch fresh weather when cache expired`() = runBlocking {
        // given
        val location = "Zurich"
        val oldWeatherResponse = createCurrentWeather(
            timestamp = OffsetDateTime.now().minusHours(2)
        )
        val oldWeather = requireNotNull(weatherService.convert(location, oldWeatherResponse))
        val freshWeatherResponse = createCurrentWeather()
        val freshWeather = requireNotNull(weatherService.convert(location, freshWeatherResponse))
        mockWeatherRepo.setCurrent(location, oldWeather)
        mockFeed.setCurrentWeather(location, freshWeatherResponse)

        // when
        val result = weatherService.current(location)

        // then
        assertNotNull(result)
        assertEquals(freshWeather, result)
        assertEquals(1, mockFeed.currentCallCount)
    }

    @Test
    fun `should store location when fetching fresh weather`() = runBlocking {
        // given
        val location = "Zurich"
        val weatherResponse = createCurrentWeather()
        val weather = requireNotNull(weatherService.convert(location, weatherResponse))
        mockFeed.setCurrentWeather(location, weatherResponse)

        // when
        weatherService.current(location)

        // then
        val storedLocation = mockLocationRepo.getPosition(location)
        assertNotNull(storedLocation)
        assertEquals(weather.coord.lat, storedLocation.latitude)
        assertEquals(weather.coord.lon, storedLocation.longitude)
    }

    private fun createCurrentWeather(
        timestamp: OffsetDateTime = OffsetDateTime.now()
    ) = CurrentWeatherResponse(
        cod = 101,
        weather = listOf(WeatherDescription(800, "Clear", "clear sky", "01d")),
        main = WeatherInfo(
            temp = 20.0f,
            temp_min = 18.0f,
            temp_max = 22.0f,
            pressure = 1015f,
            humidity = 65f
        ),
        sys = SunriseSunsetInfo(sunrise = OffsetDateTime.now(), sunset = OffsetDateTime.now().plusHours(2)),
        coord = Coord(lon = 47.3769, lat = 8.5417),
        dt = timestamp
    )
}

// Mock classes
class MockOpenWeatherFeed : WeatherFeed {
    private val weatherMap = mutableMapOf<String, CurrentWeatherResponse>()
    var currentCallCount = 0
        private set

    fun setCurrentWeather(location: String, weather: CurrentWeatherResponse) {
        weatherMap[location] = weather
    }

    override suspend fun current(location: String): CurrentWeatherResponse? {
        currentCallCount++
        return weatherMap[location]
    }

    override suspend fun forecast(location: String): ForecastWeatherResponse? {
        TODO("Not yet implemented")
    }
}

class MockWeatherRepo : WeatherRepo {
    private val storage = mutableMapOf<String, CurrentWeather>()
    var storeCallCount = 0
        private set

    fun setCurrent(location: String, weather: CurrentWeather) {
        storage[location] = weather
    }

    override suspend fun getCurrent(location: String): CurrentWeather? = storage[location]

    override suspend fun storeCurrent(weather: CurrentWeather) {
        storeCallCount++
        storage[weather.location] = weather
    }

    override suspend fun listForecast(location: String, limit: Int): List<ForecastWeather> {
        TODO("Not yet implemented")
    }

    override suspend fun storeForecast(forecast: List<ForecastWeather>) {
        TODO("Not yet implemented")
    }
}

class MockLocationRepo : LocationRepo {
    private val storage = mutableMapOf<String, GeoPosition>()
    var storeCallCount = 0
        private set

    override suspend fun store(location: String, position: GeoPosition) {
        storeCallCount++
        storage[location] = position
    }

    override suspend fun getPosition(location: String): GeoPosition? = storage[location]

    override suspend fun suggestLocations(snippet: String): List<String> =
        storage.keys.filter { it.contains(snippet, ignoreCase = true) }
}
