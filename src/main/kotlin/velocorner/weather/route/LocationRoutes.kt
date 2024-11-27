package velocorner.weather.route

import io.github.smiley4.ktorswaggerui.dsl.routing.get
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
        val location = call.parameters["location"] ?: return@get call.respondText(
            "Missing location",
            status = HttpStatusCode.BadRequest
        )
        // convert city[,country] to city[ ,isoCountry]
        val isoLocation = CountryUtil.iso(location)
        val geoLocation = repo.getPosition(isoLocation) ?: return@get call.respondText(
            "Unknown location $isoLocation",
            status = HttpStatusCode.NotFound
        )
        call.respond(geoLocation)
    }
}