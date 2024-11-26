package velocorner.weather.route

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import velocorner.weather.service.CountryFeed

fun Route.locationRoutes() {
    route("location") {
        get("ip", {
            description = "Determines the country and capital from IP address"
            tags = listOf("location")
        }) {
            val ip = call.request.headers["X-Forwarded-For"] ?: call.request.origin.remoteAddress
            val country = CountryFeed.country(ip)
            call.respondText("Hello World: $country")
        }
    }
}