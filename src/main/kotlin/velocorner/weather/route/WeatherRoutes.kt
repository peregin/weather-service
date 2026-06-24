package velocorner.weather.route

import io.ktor.http.*
import io.ktor.http.ContentType.Application.Xml
import io.ktor.server.response.*
import io.ktor.server.routing.*
import velocorner.weather.service.WeatherService
import velocorner.weather.util.toMeteoGramXml
import org.slf4j.LoggerFactory
import velocorner.weather.model.CurrentWeather
import velocorner.weather.util.CountryUtil
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.config.RouteConfig

private val logger = LoggerFactory.getLogger("WeatherRoutes")
private const val cookieAge = 60 * 60 * 24 * 7 // 7 days
private const val locationCookie = "weather_location"

private data class WeatherLocation(val raw: String, val iso: String)

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
            val location = call.weatherLocationOrNull() ?: return@get
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
            val location = call.weatherLocationOrNull() ?: return@get
            logger.debug("collecting weather forecast for [${location.raw}] -> [${location.iso}]")
            val forecast = service.forecast(location.iso)
            if (forecast.isEmpty()) return@get call.respondUnknownLocation(location.iso)
            call.rememberWeatherLocation(location.iso)
            call.respondText(toMeteoGramXml(forecast), contentType = Xml, status = HttpStatusCode.OK)
        }
    }
}

private suspend fun RoutingCall.weatherLocationOrNull(): WeatherLocation? {
    val location = parameters["location"] ?: run {
        respondText("Missing location", status = HttpStatusCode.BadRequest)
        return null
    }
    // convert city[,country] to city[ ,isoCountry]
    return WeatherLocation(raw = location, iso = CountryUtil.iso(location))
}

private suspend fun RoutingCall.respondUnknownLocation(isoLocation: String) {
    respondText("Unknown location $isoLocation", status = HttpStatusCode.NotFound)
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

internal fun RouteConfig.setupLocationParameter() {
    request {
        pathParameter<String>("location") {
            description = "Location in format: city[,isoCountry 2-letter code]"
            example("Zurich") {
                value = "Zurich,CH"
            }
        }
    }
}

internal fun RouteConfig.setupCommonResponses() {
    response {
        HttpStatusCode.BadRequest to { description = "Missing location" }
        HttpStatusCode.NotFound to { description = "Unknown location" }
    }
}
