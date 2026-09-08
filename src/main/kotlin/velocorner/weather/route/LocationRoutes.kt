package velocorner.weather.route

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import velocorner.weather.model.GeoLocationResponse
import velocorner.weather.model.GeoPosition
import velocorner.weather.model.SuggestionResponse
import velocorner.weather.repo.LocationRepo
import velocorner.weather.service.CountryFeed
import velocorner.weather.util.CountryUtil

fun Route.locationRoutes(repo: LocationRepo) {
    route("location") {
        get("ip", {
            description = "Determines the country and city from IP address, when city is not available defaults to capital"
            tags = listOf("location")
            request {
                queryParameter<String>("ip") {
                    description = "Optional IP address. Falls back to X-Forwarded-For or remote address when omitted."
                    required = false
                    example("IPv4") {
                        value = "8.8.8.8"
                    }
                }
            }
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
            call.respondGeoLocation()
        }

        get("suggest", {
            description = "Suggests a list of cities and countries"
            tags = listOf("location")
            request {
                queryParameter<String>("query") {
                    description = "The query used to suggest"
                    required = true
                    example("Zurich") {
                        value = "Zur"
                    }
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Suggests a list of countries"
                    body<List<String>> {
                        description = "The result of the location"
                        example("Germany")
                        {
                            value = listOf("DE", "FR")
                        }
                    }
                }
                HttpStatusCode.BadRequest to { description = "Missing query" }
            }
        }) {
            val query = call.request.queryParameters["query"] ?: return@get call.respondText(
                "Missing query",
                status = HttpStatusCode.BadRequest
            )
            val suggestions = repo.suggestLocations(query)
                .let(CountryUtil::normalize)
                .map(CountryUtil::beautify)
                .take(10) // Limit results for better performance
                .let(::SuggestionResponse)
            call.respond(suggestions)
        }

        get("geo/{location}", {
            description = "Determines the country and capital from location"
            tags(listOf("location"))
            request {
                this@get.setupLocationParameter()
            }
            response {
                HttpStatusCode.OK to {
                    description = "The geolocation of the given input"
                    body<GeoPosition> {
                        description = "The geolocation as latitude, longitude"
                    }
                }
                this@get.setupCommonResponses()
            }
        }) {
            val location = call.locationParameterOrNull() ?: return@get
            val geoLocation = repo.getPosition(location.iso) ?: return@get call.respondUnknownLocation(location.iso)
            call.respond(geoLocation)
        }
    }
}

private suspend fun RoutingCall.respondGeoLocation() {
    val ip = parameters["ip"]
        ?: request.queryParameters["ip"]
        ?: request.headers["X-Forwarded-For"]
        ?: request.origin.remoteAddress
    val reply = CountryFeed.country(ip)
    val country = reply.country
    val city = reply.city ?: CountryUtil.code2Capital[country] ?: throw NotFoundException("country $country not found")
    respond(GeoLocationResponse(city = city, country = country))
}
