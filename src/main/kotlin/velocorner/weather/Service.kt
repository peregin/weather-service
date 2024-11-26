package velocorner.weather

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import velocorner.weather.repo.DatabaseFactory
import velocorner.weather.repo.WeatherRepoImpl
import velocorner.weather.route.*
import velocorner.weather.service.OpenWeatherFeed
import velocorner.weather.service.WeatherService

fun main() {
    embeddedServer(Netty, port = 9015, host = "0.0.0.0") {
        log.info("starting weather service...")

        val feed = OpenWeatherFeed()
        DatabaseFactory.init()
        val repo = WeatherRepoImpl()
        val service = WeatherService(feed, repo)

        install(ContentNegotiation) {
            json()
        }
        install(CallLogging)
        install(SwaggerUI) {
            info {
                title = "Weather API"
                version = "latest"
                description = "Weather forecast"
            }
            pathFilter = { _, url: List<String> ->
                url.any { it.contains("weather", ignoreCase = true) }
            }
        }

        routing {
            staticResources("/", "static")
            welcomeRoutes()
            weatherRoutes(service)
            route("api.json") {
                openApiSpec()
            }
            route("docs") {
                swaggerUI("/api.json")
            }
        }
    }.start(wait = true)
}
