package velocorner.weather.service

import org.slf4j.LoggerFactory
import velocorner.weather.model.CurrentWeather
import velocorner.weather.model.CurrentWeatherResponse
import velocorner.weather.model.ForecastWeather
import velocorner.weather.model.ForecastWeatherResponse
import velocorner.weather.repo.WeatherRepo
import velocorner.weather.util.WeatherCodeUtil
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// it uses data from the cache/storage if was queried within the `refreshTimeout`
class WeatherService(val feed: OpenWeatherFeed, val repo: WeatherRepo, val refreshTimeout: Duration = 30.minutes) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun clock(): OffsetDateTime = OffsetDateTime.now(ZoneId.of("UTC"))

    suspend fun current(location: String): CurrentWeather? {
        val entry = repo.getCurrent(location)
        val reply =
            entry?.takeIf {
                val diffInSeconds = (clock().toEpochSecond() - it.timestamp.toEpochSecond())
                    .seconds
                    .compareTo(refreshTimeout)
                diffInSeconds > 0
            }.also {
                logger.debug("retrieving cached data for [$location]")
            } ?: feed.current(location).let { re ->
                convert(location, re).also{
                    logger.info("retrieving fresh data for [$location]")
                    it?.let { repo.storeCurrent(it) }
                }
            }
        return reply
    }

    suspend fun forecast(location: String): List<ForecastWeather>? {
        val reply = feed.forecast(location)
        return reply?.let { convert(location, it) }
    }

    private fun convert(location: String, reply: CurrentWeatherResponse?): CurrentWeather? {
        return reply?.weather?.let { wd ->
            reply.sys?.let { sy ->
                reply.main?.let { ma ->
                    reply.coord?.let { co ->
                        CurrentWeather(
                            location = location,
                            timestamp = reply.dt ?: OffsetDateTime.now(ZoneId.of("UTC")),
                            bootstrapIcon = WeatherCodeUtil.bootstrapIcon(wd[0].id),
                            current = wd[0],
                            info = ma,
                            sunriseSunset = sy,
                            coord = co
                        )
                    }
                }
            }
        }
    }

    private fun convert(location: String, reply: ForecastWeatherResponse): List<ForecastWeather> {
        return reply.list?.map {
            ForecastWeather(
                location = location,
                timestamp = it.dt,
                forecast = it
            )
        } ?: emptyList()
    }
}
