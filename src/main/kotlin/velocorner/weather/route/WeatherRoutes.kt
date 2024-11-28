package velocorner.weather.route

import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Xml
import io.ktor.server.response.*
import io.ktor.server.routing.*
import velocorner.weather.service.WeatherService
import velocorner.weather.util.toMeteoGramXml
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.util.date.*
import org.slf4j.LoggerFactory
import velocorner.weather.model.CurrentWeather
import velocorner.weather.util.CountryUtil

private val logger = LoggerFactory.getLogger("WeatherRoutes")
private const val cookieAge = 60 * 60 * 24 * 7 // 7 days

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
            val location = call.parameters["location"] ?: return@get call.respondText(
                "Missing location",
                status = HttpStatusCode.BadRequest
            )
            // convert city[,country] to city[ ,isoCountry]
            val isoLocation = CountryUtil.iso(location)
            logger.debug("collecting current weather for [$location] -> [$isoLocation]")
            val current = service.current(isoLocation) ?: return@get call.respondText(
                "Unknown location $isoLocation",
                status = HttpStatusCode.NotFound
            )
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
            val location = call.parameters["location"] ?: return@get call.respondText(
                "Missing location",
                status = HttpStatusCode.BadRequest
            )
            // convert city[,country] to city[ ,isoCountry]
            val isoLocation = CountryUtil.iso(location)
            logger.debug("collecting weather forecast for [$location] -> [$isoLocation]")
            val forecast = service.forecast(isoLocation)
            if (forecast.isEmpty()) return@get call.respondText(
                "Unknown location $isoLocation",
                status = HttpStatusCode.NotFound
            )
            // remove old cookie set by the Scala web-app
            call.response.cookies.append(
                Cookie(
                    name = "weather_location",
                    encoding = CookieEncoding.BASE64_ENCODING,
                    value = "",
                    path = "/",
                    domain = "velocorner.com",
                    maxAge = 0,
                    expires = GMTDate.START
                )
            )
            // add a cookie, it is read by the frontend to lock the once set location for forecast
            call.response.cookies.append(
                Cookie(
                    name = "weather_location",
                    encoding = CookieEncoding.BASE64_ENCODING,
                    value = isoLocation,
                    path = "/",
                    domain = ".velocorner.com",
                    maxAge = cookieAge
                )
            )
            call.respondText(toMeteoGramXml(forecast), contentType = Xml, status = HttpStatusCode.OK)
        }
    }
}

internal fun OpenApiRoute.setupLocationParameter() {
    request {
        pathParameter<String>("location") {
            description = "Location in format: city[,isoCountry 2-letter code]"
            example("Zurich") {
                value = "Zurich,CH"
            }
        }
    }
}

internal fun OpenApiRoute.setupCommonResponses() {
    response {
        HttpStatusCode.BadRequest to { description = "Missing location" }
        HttpStatusCode.NotFound to { description = "Unknown location" }
    }
}
