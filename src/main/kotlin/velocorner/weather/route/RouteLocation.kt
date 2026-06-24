package velocorner.weather.route

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import velocorner.weather.util.CountryUtil

internal data class RouteLocation(val raw: String, val iso: String)

internal suspend fun RoutingCall.locationParameterOrNull(): RouteLocation? {
    val location = parameters["location"] ?: run {
        respondText("Missing location", status = HttpStatusCode.BadRequest)
        return null
    }
    // convert city[,country] to city[ ,isoCountry]
    return RouteLocation(raw = location, iso = CountryUtil.iso(location))
}

internal suspend fun RoutingCall.respondUnknownLocation(isoLocation: String) {
    respondText("Unknown location $isoLocation", status = HttpStatusCode.NotFound)
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
