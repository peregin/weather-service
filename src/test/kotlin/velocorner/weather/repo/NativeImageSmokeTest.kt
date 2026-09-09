package velocorner.weather.repo

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import velocorner.weather.model.*
import velocorner.weather.util.ResourceUtil.load
import velocorner.weather.util.WeatherCodeUtil
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Runs only through nativeSmokeTest, against a disposable Testcontainers database. */
internal class NativeImageSmokeTest {
    @Test
    fun `native executable migrates a fresh database and serves application routes`() {
        // Fail before launching anything if another application owns the service port.
        ServerSocket().use { it.bind(InetSocketAddress("127.0.0.1", 9015)) }
        val executable = requireNotNull(System.getProperty("weather.native.executable"))
        val database = TestDatabase.start()
        val config = database.config
        val log = File("build/reports/tests/nativeSmokeTest/server.log").apply { parentFile.mkdirs() }
        var process: Process? = null
        try {
            process = ProcessBuilder(executable).apply {
                environment().putAll(mapOf(
                    "WEATHER_API_KEY" to "native-smoke-test",
                    "DB_DRIVER" to config.getString("db.driver"),
                    "DB_URL" to config.getString("db.url"),
                    "DB_USER" to config.getString("db.user"),
                    "DB_PASSWORD" to config.getString("db.password")
                ))
                redirectErrorStream(true)
                redirectOutput(log)
            }.start()
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build().use { client ->
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
                while (true) {
                    check(process.isAlive) { "Native server exited before becoming ready" }
                    if (runCatching { get(client, "/health").statusCode() == 200 }.getOrDefault(false)) break
                    check(System.nanoTime() < deadline) { "Native server did not become ready within 60 seconds" }
                    Thread.sleep(200)
                }

                // Check before any JVM-side Flyway initialization: the native binary must migrate itself.
                DriverManager.getConnection(config.getString("db.url"), config.getString("db.user"),
                    config.getString("db.password")).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT \"version\" FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"").use { rows ->
                            val versions = buildList { while (rows.next()) add(rows.getString(1)) }
                            assertEquals(listOf("1", "2"), versions)
                        }
                    }
                }
                seedWeather()

                assertContains(ok(client, "/"), "Weather Service")
                assertContains(ok(client, "/welcome.css"), ".hero")
                assertContains(ok(client, "/docs/index.html"), "swagger-ui-bundle.js")
                assertTrue(ok(client, "/docs/swagger-ui.css").length > 1000)
                assertTrue(ok(client, "/docs/swagger-ui-bundle.js").length > 1000)

                val specText = ok(client, "/api.json")
                File(log.parentFile, "api.json").writeText(specText)
                val spec = Json.parseToJsonElement(specText).jsonObject
                assertEquals("3.1.0", spec.getValue("openapi").jsonPrimitive.content)
                assertTrue(spec.getValue("paths").jsonObject.containsKey("/weather/current/{location}"))
                val schemas = spec.getValue("components").jsonObject.getValue("schemas").jsonObject
                assertTrue(schemas.isNotEmpty())
                assertTrue(schemas.values.any { it.jsonObject["type"]?.jsonPrimitive?.content == "object" })
                assertFalse(spec.toString().contains("exampleSetFlag"), "Swagger Jackson mixins must be retained")
                assertFalse(spec.toString().contains("defaultSetFlag"), "Swagger field annotations must be retained")

                val suggestions = Json.parseToJsonElement(ok(client, "/location/suggest?query=Zur")).jsonObject
                assertTrue(suggestions.getValue("suggestions").jsonArray.isNotEmpty())
                val position = Json.decodeFromString<GeoPosition>(ok(client, "/location/geo/Zurich,CH"))
                assertEquals(47.3769, position.latitude)
                val current = Json.decodeFromString<CurrentWeather>(ok(client, "/weather/current/Zurich,CH"))
                assertEquals("Zurich,CH", current.location)
                assertContains(ok(client, "/weather/forecast/Zurich,CH"), "<weatherdata>")
                assertEquals(400, get(client, "/location/suggest").statusCode())
                assertEquals(404, get(client, "/location/geo/Unknown,CH").statusCode())
            }
        } catch (failure: Throwable) {
            throw AssertionError("Native smoke test failed. Server log: ${log.absolutePath}\n${log.takeIf { it.exists() }?.readText()}", failure)
        } finally {
            process?.let {
                it.destroy()
                if (!it.waitFor(10, TimeUnit.SECONDS)) {
                    it.destroyForcibly()
                    it.waitFor(10, TimeUnit.SECONDS)
                }
            }
            database.container.stop()
        }
    }

    private fun seedWeather() = runBlocking {
        DatabaseFactory.init(TestDatabase.config())
        val location = "Zurich,CH"
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val fixture = load<CurrentWeatherResponse>("/current.json")
        val description = requireNotNull(fixture.weather).first()
        WeatherRepoImpl().storeCurrent(CurrentWeather(
            location = location, timestamp = now,
            bootstrapIcon = WeatherCodeUtil.bootstrapIcon(description.id),
            reactIcon = WeatherCodeUtil.reactIcon(description.id), current = description,
            info = requireNotNull(fixture.main), sunriseSunset = requireNotNull(fixture.sys),
            coord = requireNotNull(fixture.coord)
        ))
        val forecast = requireNotNull(load<ForecastWeatherResponse>("/forecast.json").list).first().copy(dt = now)
        WeatherRepoImpl().storeForecast(listOf(ForecastWeather(location, now, forecast)))
        LocationRepoImpl().store(location, GeoPosition(47.3769, 8.5417))
    }

    private fun get(client: HttpClient, path: String): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:9015$path")).timeout(Duration.ofSeconds(10)).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    )

    private fun ok(client: HttpClient, path: String): String {
        val response = get(client, path)
        assertEquals(200, response.statusCode(), "$path: ${response.body()}")
        return response.body()
    }
}
