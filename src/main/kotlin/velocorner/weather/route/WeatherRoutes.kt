package velocorner.weather.route

import io.ktor.http.*
import io.ktor.http.ContentType.Application.Xml
import io.ktor.server.response.*
import io.ktor.server.routing.*
import velocorner.weather.service.WeatherService
import velocorner.weather.util.toMeteoGramXml
import org.slf4j.LoggerFactory
import velocorner.weather.model.CurrentWeather
import io.github.smiley4.ktoropenapi.get

private val logger = LoggerFactory.getLogger("WeatherRoutes")
private const val cookieAge = 60 * 60 * 24 * 7 // 7 days
private const val locationCookie = "weather_location"

// location is in format: city[,isoCountry 2-letter code]
fun Route.weatherRoutes(service: WeatherService) {
    route("weather") {

        // retrieves the sunrise and sunset information for a given place
        get("current/{location}", {
            description = "Get current weather"
            tags = listOf("weather")
            request {
                this@get.setupLocationParameter()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Current weather"
                    body<CurrentWeather> {
                        description = "The result of the current weather query"
                    }
                }
                this@get.setupCommonResponses()
            }
        }) {
            val location = call.locationParameterOrNull() ?: return@get
            logger.debug("collecting current weather for [${location.raw}] -> [${location.iso}]")
            val current = service.current(location.iso) ?: return@get call.respondUnknownLocation(location.iso)
            call.rememberWeatherLocation(location.iso)
            call.respond(current)
        }

        // retrieves the weather forecast for a given place
        get("forecast/{location}", {
            description = "Get forecast"
            tags = listOf("weather")
            request {
                this@get.setupLocationParameter()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Forecast"
                }
                this@get.setupCommonResponses()
            }
        }) {
            val location = call.locationParameterOrNull() ?: return@get
            logger.debug("collecting weather forecast for [${location.raw}] -> [${location.iso}]")
            val forecast = service.forecast(location.iso)
            if (forecast.isEmpty()) return@get call.respondUnknownLocation(location.iso)
            call.rememberWeatherLocation(location.iso)
            call.respondText(toMeteoGramXml(forecast), contentType = Xml, status = HttpStatusCode.OK)
        }
    }
}

private fun RoutingCall.rememberWeatherLocation(isoLocation: String) {
    // Read by the frontend to lock the once set location for forecast.
    response.cookies.append(
        Cookie(
            name = locationCookie,
            encoding = CookieEncoding.BASE64_ENCODING,
            value = isoLocation,
            path = "/",
            domain = ".velocorner.com",
            maxAge = cookieAge
        )
    )
}
