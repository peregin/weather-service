package velocorner.weather.route

import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Xml
import io.ktor.server.response.*
import io.ktor.server.routing.*
import velocorner.weather.service.WeatherService
import velocorner.weather.util.toMeteoGramXml
import io.github.smiley4.ktorswaggerui.dsl.routing.get

// location is in format: city[,isoCountry 2-letter code]
fun Route.weatherRoutes(service: WeatherService) {
    route("weather") {
        get("current/{location}", {
            description = "Get current weather"
            tags = listOf("weather")
            request {
                this@get.setupLocationParameter()
            }
            response {
                HttpStatusCode.OK to { description = "Current weather" }
                this@get.setupCommonResponses()
            }
        }) {
            val location = call.parameters["location"] ?: return@get call.respondText(
                "Missing location",
                status = HttpStatusCode.BadRequest
            )
            val current = service.current(location) ?: return@get call.respondText(
                "Unknown location $location",
                status = HttpStatusCode.NotFound
            )
            call.respond(current)
        }
        get("forecast/{location}", {
            description = "Get forecast"
            tags = listOf("weather")
            request {
                this@get.setupLocationParameter()
            }
            response {
                HttpStatusCode.OK to { description = "Forecast" }
                this@get.setupCommonResponses()
            }
        }) {
            val location = call.parameters["location"] ?: return@get call.respondText(
                "Missing location",
                status = HttpStatusCode.BadRequest
            )
            val forecast = service.forecast(location)
            if (forecast.isEmpty()) return@get call.respondText(
                "Unknown location $location",
                status = HttpStatusCode.NotFound
            )
            call.respondText(toMeteoGramXml(forecast), contentType = Xml, status = HttpStatusCode.OK)
        }
    }
}

private fun OpenApiRoute.setupLocationParameter() {
    request {
        pathParameter<String>("location") {
            description = "Location in format: city[,isoCountry 2-letter code]"
            example("Zurich") {
                value = "Zurich,CH"
            }
        }
    }
}

private fun OpenApiRoute.setupCommonResponses() {
    response {
        HttpStatusCode.BadRequest to { description = "Missing location" }
        HttpStatusCode.NotFound to { description = "Unknown location" }
    }
}
