package velocorner.weather.service

import org.slf4j.LoggerFactory
import velocorner.weather.model.*
import velocorner.weather.repo.LocationRepo
import velocorner.weather.repo.WeatherRepo
import velocorner.weather.util.WeatherCodeUtil
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// it uses data from the cache/storage if was queried within the `refreshTimeout`
class WeatherService(val feed: WeatherFeed, val weatherRepo: WeatherRepo, val locationRepo: LocationRepo, val refreshTimeout: Duration = 60.minutes) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private fun clock(): OffsetDateTime = OffsetDateTime.now(ZoneId.of("UTC"))

    suspend fun current(location: String): CurrentWeather? {
        val entry = weatherRepo.getCurrent(location)
        val reply =
            entry?.takeUnless {
                val now = clock()
                val last = it.timestamp
                logger.debug("checking current weather cache $now - $last")
                val cacheHit = (now.toEpochSecond() - last.toEpochSecond())
                    .seconds
                    .compareTo(refreshTimeout)
                cacheHit > 0
            }.also {
                logger.debug("retrieving cached data for current [$location]")
            } ?: feed.current(location).let { re ->
                convert(location, re).also {
                    logger.info("retrieving and store fresh data for current [$location]")
                    it?.let {
                        weatherRepo.storeCurrent(it)
                        // store into the locations storage for suggestions and Windy
                        val geoLocation = GeoPosition(latitude = it.coord.lat, longitude = it.coord.lon)
                        locationRepo.store(location, geoLocation)
                    }
                }
            }
        return reply
    }

    suspend fun forecast(location: String): List<ForecastWeather> {
        val entries = weatherRepo.listForecast(location) // takes 40 entries, latest is now
        val last = entries.map { it.timestamp }.minOrNull()?.takeUnless {
            val now = clock()
            logger.debug("checking forecast weather cache $now - $it")
            val cacheHit = (now.toEpochSecond() - it.toEpochSecond())
                .seconds
                .compareTo(refreshTimeout)
            cacheHit > 0
        }

        val reply = last?.let { entries }.also {
            logger.debug("retrieving cached data for forecast [$location]")
        } ?: run {
            feed.forecast(location).let { re ->
                convert(location, re).also {
                    logger.info("retrieving and store fresh data for forecast [$location]")
                    weatherRepo.storeForecast(it)
                }
            }
        }
        return reply
    }

    internal fun convert(location: String, reply: CurrentWeatherResponse?): CurrentWeather? {
        return reply?.let { r ->
            with(r) {
                if (weather != null && sys != null && main != null && coord != null) {
                    CurrentWeather(
                        location = location,
                        timestamp = reply.dt ?: OffsetDateTime.now(ZoneId.of("UTC")),
                        bootstrapIcon = WeatherCodeUtil.bootstrapIcon(weather[0].id),
                        current = weather[0],
                        info = main,
                        sunriseSunset = sys,
                        coord = coord
                    )
                } else null
            }
        }
    }

    internal fun convert(location: String, reply: ForecastWeatherResponse?): List<ForecastWeather> {
        return reply?.list?.map {
            ForecastWeather(
                location = location,
                timestamp = it.dt,
                forecast = it
            )
        } ?: emptyList()
    }
}
