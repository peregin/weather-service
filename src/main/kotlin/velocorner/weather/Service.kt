package velocorner.weather

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import velocorner.weather.route.*
import velocorner.weather.service.OpenWeatherFeed

fun main() {
    embeddedServer(Netty, port = 9015, host = "0.0.0.0") {
        log.info("loading config...")
        val config = ConfigFactory.load()
        val apiKey = config.getString("weather.application.id")
        log.info("OpenWeatherApi key is [${apiKey.takeLast(4).padStart(apiKey.length, 'X')}]")
        val client = HttpClient()
        val feed = OpenWeatherFeed(apiKey, client)

        install(ContentNegotiation) {
            json()
        }
        install(CallLogging)

        routing {
            welcomeRoutes()
            weatherRoutes(feed)
        }
    }.start(wait = true)
}