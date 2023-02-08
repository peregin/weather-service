package velocorner.weather

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import velocorner.weather.repo.DatabaseFactory
import velocorner.weather.route.*
import velocorner.weather.service.OpenWeatherFeed
import java.io.File

fun main() {
    embeddedServer(Netty, port = 9015, host = "0.0.0.0") {
        val configPath = System.getenv("CONFIG_FILE")
        log.info("config_file=$configPath")
        val config = ConfigFactory.parseFile(File(configPath))

        val client = HttpClient(Java)
        val feed = OpenWeatherFeed(config, client)
        DatabaseFactory.init(config)

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