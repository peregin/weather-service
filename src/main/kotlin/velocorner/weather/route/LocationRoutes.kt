package velocorner.weather.route

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import velocorner.weather.model.GeoLocationResponse
import velocorner.weather.service.CountryFeed
import velocorner.weather.util.CountryUtil

fun Route.locationRoutes() {
    route("location") {
        get("ip", {
            description = "Determines the country and capital from IP address"
            tags = listOf("location")
            response {
                HttpStatusCode.OK to {
                    description = "City, Country"
                    body<GeoLocationResponse> {
                        description = "The result of the location"
                        example("Germany")
                        {
                            value = GeoLocationResponse(
                                city = "Berlin",
                                country = "DE"
                            )
                        }
                    }
                }
                HttpStatusCode.NotFound to { description = "Country not found" }
            }
        }) {
            val ip = call.request.headers["X-Forwarded-For"] ?: call.request.origin.remoteAddress
            val country = CountryFeed.country(ip)
            val capital = CountryUtil.code2Capital[country]
                ?: throw NotFoundException("country $country not found")
            call.respond(GeoLocationResponse(city = capital, country = country))
        }
    }
}