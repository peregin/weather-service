package velocorner.weather.repo

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import velocorner.weather.model.CurrentWeather
import velocorner.weather.model.CurrentWeatherResponse
import velocorner.weather.model.ForecastWeather
import velocorner.weather.model.ForecastWeatherResponse
import velocorner.weather.util.ResourceUtil.load
import velocorner.weather.util.WeatherCodeUtil
import kotlin.test.*
import kotlin.test.Test

internal class WeatherRepoTest {

    companion object {
        private const val ZH_LOCATION = "Zurich,CH"
        private const val BP_LOCATION = "Budapest,HU"

        @BeforeClass
        @JvmStatic
        fun setupSpec() {
            TestDatabase.start()
        }
    }

    private val currentFixture by lazy { load<CurrentWeatherResponse>("/current.json") }
    private val forecastFixture by lazy { load<ForecastWeatherResponse>("/forecast.json") }

    @Before
    fun setup() {
        DatabaseFactory.init(config = TestDatabase.config())
        truncateTables()
    }

    private fun truncateTables() = runBlocking {
        TestDatabase.truncateWeatherTables()
    }

    @Test
    fun `should return null for unknown locations`() = runBlocking {
        val repo = WeatherRepoImpl()
        assertNull(repo.getCurrent("unknown, loc"))
        assertNull(repo.getCurrent("anything"))
        assertNull(repo.getCurrent(BP_LOCATION))
        assertNull(repo.getCurrent(ZH_LOCATION))
    }

    @Test
    fun upsertCurrentWeather() = runBlocking {
        val repo = WeatherRepoImpl()
        val weather = CurrentWeather(
            location = ZH_LOCATION,
            timestamp = requireNotNull(currentFixture.dt),
            bootstrapIcon = WeatherCodeUtil.bootstrapIcon(requireNotNull(currentFixture.weather).first().id),
            reactIcon = WeatherCodeUtil.reactIcon(requireNotNull(currentFixture.weather).first().id),
            current = requireNotNull(currentFixture.weather).first(),
            info = requireNotNull(currentFixture.main),
            sunriseSunset = requireNotNull(currentFixture.sys),
            coord = requireNotNull(currentFixture.coord)
        )
        repo.storeCurrent(weather)
        assertEquals(weather, repo.getCurrent(ZH_LOCATION))

        val updatedWeather = weather.copy(
            timestamp = weather.timestamp.plusMinutes(30),
            info = weather.info.copy(temp = weather.info.temp + 1)
        )
        repo.storeCurrent(updatedWeather)

        assertEquals(updatedWeather, repo.getCurrent(ZH_LOCATION))
        val entries = TestDatabase.currentWeatherCount()
        assertEquals(1, entries)
    }

    @Test
    fun emptyForecastForUnknownLocation() = runBlocking {
        val repo = WeatherRepoImpl()
        val entries = repo.listForecast("unknown, loc")
        assertEquals(0, entries.size)
    }

    @Test
    fun `should upsert forecast weather`() = runBlocking {
        val repo = WeatherRepoImpl()
        val forecastList = requireNotNull(forecastFixture.list) { "Forecast list should not be null" }
        assertEquals(40, forecastList.size)
        repo.storeForecast(forecastList.map { e ->
            ForecastWeather(
                location = ZH_LOCATION,
                timestamp = e.dt,
                forecast = e
            )
        }
        )

        assertEquals(40, repo.listForecast(ZH_LOCATION).size)
        assertEquals(0, repo.listForecast(BP_LOCATION).size)

        val first = forecastList.first()
        val updatedForecast = ForecastWeather(
            location = ZH_LOCATION,
            timestamp = first.dt,
            forecast = first.copy(main = first.main.copy(temp = first.main.temp + 1))
        )
        repo.storeForecast(listOf(updatedForecast))

        val storedForecasts = repo.listForecast(ZH_LOCATION, limit = 50)
        assertEquals(40, storedForecasts.size)
        assertEquals(updatedForecast, storedForecasts.single { it.timestamp == first.dt })

        // different location, same timestamp
        repo.storeForecast(listOf(ForecastWeather(BP_LOCATION, first.dt, first)))
        assertEquals(40, repo.listForecast(ZH_LOCATION, limit = 50).size)
        assertEquals(1, repo.listForecast(BP_LOCATION, limit = 50).size)
    }
}
