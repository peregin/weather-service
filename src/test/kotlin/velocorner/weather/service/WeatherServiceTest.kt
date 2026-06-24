package velocorner.weather.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import velocorner.weather.model.*
import velocorner.weather.repo.LocationRepo
import velocorner.weather.repo.WeatherRepo
import java.time.OffsetDateTime
import java.time.ZoneOffset

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

    @Test
    fun `should convert forecast weather to current weather with today min and max temperature`() {
        // given
        val location = "Zurich"
        val now = OffsetDateTime.parse("2026-06-24T10:30:00Z")
        val forecastResponse = createForecastWeather(now)

        // when
        val weather = requireNotNull(weatherService.convert(now, location, forecastResponse))

        // then
        assertEquals(location, weather.location)
        assertEquals(now, weather.timestamp)
        assertEquals(WeatherDescription(800, "Clear", "clear sky", "01d"), weather.current)
        assertEquals(23.0f, weather.info.temp)
        assertEquals(8.0f, weather.info.temp_min)
        assertEquals(26.0f, weather.info.temp_max)
        assertEquals(forecastResponse.city?.coord, weather.coord)
        assertEquals(forecastResponse.city?.sunrise, weather.sunriseSunset.sunrise)
        assertEquals(forecastResponse.city?.sunset, weather.sunriseSunset.sunset)
    }

    @Test
    fun `should fetch fresh current weather from forecast when available`() = runBlocking {
        // given
        val location = "Zurich"
        mockFeed.setForecastWeather(location, createForecastWeather(OffsetDateTime.now(ZoneOffset.UTC)))

        // when
        val result = requireNotNull(weatherService.current(location))

        // then
        assertEquals(1, mockFeed.forecastCallCount)
        assertEquals(0, mockFeed.currentCallCount)
        assertNotNull(mockLocationRepo.getPosition(location))
        assertEquals(location, result.location)
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

    private fun createForecastWeather(
        now: OffsetDateTime,
        timezone: ZoneOffset = ZoneOffset.UTC
    ): ForecastWeatherResponse {
        val today = now.withOffsetSameInstant(timezone).toLocalDate()
        fun forecastTime(hour: Int) = today.atTime(hour, 0).atOffset(timezone)
        return ForecastWeatherResponse(
            cod = "200",
            city = City(
                id = 2657896,
                name = "Zurich",
                country = "CH",
                coord = Coord(lon = 8.5417, lat = 47.3769),
                timezone = timezone.totalSeconds,
                sunrise = today.atTime(5, 30).atOffset(timezone),
                sunset = today.atTime(21, 30).atOffset(timezone)
            ),
            list = listOf(
                createForecastWindow(forecastTime(0), 10.0f, 8.0f, 12.0f, 801, "Clouds", "few clouds", "02n"),
                createForecastWindow(forecastTime(9), 18.0f, 16.0f, 20.0f, 802, "Clouds", "scattered clouds", "03d"),
                createForecastWindow(forecastTime(12), 23.0f, 21.0f, 26.0f, 800, "Clear", "clear sky", "01d"),
                createForecastWindow(forecastTime(18), 20.0f, 17.0f, 22.0f, 801, "Clouds", "few clouds", "02d"),
                createForecastWindow(forecastTime(0).plusDays(1), 30.0f, 1.0f, 40.0f, 800, "Clear", "clear sky", "01n")
            )
        )
    }

    private fun createForecastWindow(
        timestamp: OffsetDateTime,
        temp: Float,
        tempMin: Float,
        tempMax: Float,
        weatherId: Int,
        main: String,
        description: String,
        icon: String
    ) = Weather(
        dt = timestamp,
        main = WeatherInfo(
            temp = temp,
            temp_min = tempMin,
            temp_max = tempMax,
            pressure = 1015f,
            humidity = 65f
        ),
        weather = listOf(WeatherDescription(weatherId, main, description, icon)),
        clouds = CloudDescription(all = 20),
        wind = WindDescription(speed = 2.5, deg = 180.0)
    )
}

// Mock classes
class MockOpenWeatherFeed : WeatherFeed {
    private val weatherMap = mutableMapOf<String, CurrentWeatherResponse>()
    private val forecastMap = mutableMapOf<String, ForecastWeatherResponse>()
    var currentCallCount = 0
        private set
    var forecastCallCount = 0
        private set

    fun setCurrentWeather(location: String, weather: CurrentWeatherResponse) {
        weatherMap[location] = weather
    }

    fun setForecastWeather(location: String, forecast: ForecastWeatherResponse) {
        forecastMap[location] = forecast
    }

    override suspend fun current(location: String): CurrentWeatherResponse? {
        currentCallCount++
        return weatherMap[location]
    }

    override suspend fun forecast(location: String): ForecastWeatherResponse? {
        forecastCallCount++
        return forecastMap[location]
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
