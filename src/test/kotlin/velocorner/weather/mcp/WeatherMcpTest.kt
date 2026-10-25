package velocorner.weather.mcp

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import kotlinx.serialization.json.*
import velocorner.weather.model.*
import velocorner.weather.repo.WeatherRepo
import velocorner.weather.route.healthRoutes
import velocorner.weather.route.weatherRoutes
import velocorner.weather.service.MockLocationRepo
import velocorner.weather.service.WeatherFeed
import velocorner.weather.service.WeatherService
import velocorner.weather.util.ResourceUtil.load
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.*

internal class WeatherMcpTest {
    @Test
    fun `initializes and discovers tools over stateless HTTP`() = testApplication {
        configure(Fixture())
        val initialized = client.rpc("initialize", buildJsonObject {
            put("protocolVersion", "2025-11-25")
            put("capabilities", buildJsonObject {})
            put("clientInfo", buildJsonObject {
                put("name", "weather-test")
                put("version", "1.0.0")
            })
        })
        assertFalse(initialized.containsKey("error"))
        val result = initialized.getValue("result").jsonObject
        assertEquals("weather-service", result.getValue("serverInfo").jsonObject["name"]?.jsonPrimitive?.content)
        assertTrue(result.getValue("capabilities").jsonObject.containsKey("tools"))

        // Requests remain independent: subsequent calls do not need a session header.
        val tools = client.rpc("tools/list").getValue("result").jsonObject.getValue("tools").jsonArray
        assertEquals(setOf("get_current_weather", "get_weather_forecast"), tools.map {
            it.jsonObject.getValue("name").jsonPrimitive.content
        }.toSet())
        tools.forEach {
            val schema = it.jsonObject.getValue("inputSchema").jsonObject
            assertEquals("object", schema.getValue("type").jsonPrimitive.content)
            assertEquals(listOf(JsonPrimitive("location")), schema.getValue("required").jsonArray.toList())
        }
        assertEquals(HttpStatusCode.MethodNotAllowed, client.get("/mcp") {
            header(HttpHeaders.Host, "localhost")
        }.status)
        assertEquals(HttpStatusCode.MethodNotAllowed, client.delete("/mcp") {
            header(HttpHeaders.Host, "localhost")
        }.status)
    }

    @Test
    fun `current weather normalizes location and returns cached structured data`() = testApplication {
        val fixture = Fixture()
        configure(fixture)
        val result = client.callTool("get_current_weather", buildJsonObject {
            put("location", " Zurich,Switzerland ")
        })
        assertFalse(result.getValue("isError").jsonPrimitive.boolean)
        val weather = result.getValue("structuredContent").jsonObject
        assertEquals(Json.encodeToJsonElement(fixture.current), weather)
        assertEquals(fixture.current.timestamp.toEpochSecond(), weather.getValue("timestamp").jsonPrimitive.long)
        assertEquals(weather, Json.parseToJsonElement(result.errorText()))
        assertTrue(fixture.feed.requests.isEmpty())

        // A later request must still work after the previous transport is closed.
        assertFalse(client.callTool("get_current_weather", locationArguments()).getValue("isError").jsonPrimitive.boolean)
    }

    @Test
    fun `forecast returns JSON intervals and preserves model serialization`() = testApplication {
        val fixture = Fixture()
        configure(fixture)
        val result = client.callTool("get_weather_forecast", locationArguments())
        assertFalse(result.getValue("isError").jsonPrimitive.boolean)
        val data = result.getValue("structuredContent").jsonObject
        assertEquals(Json.encodeToJsonElement(listOf(fixture.forecast)), data.getValue("forecast"))
        assertEquals(data, Json.parseToJsonElement(result.errorText()))
        assertTrue(fixture.feed.requests.isEmpty())
    }

    @Test
    fun `invalid locations do not access the weather service`() = testApplication {
        val fixture = Fixture()
        configure(fixture)
        val invalidArguments = listOf(
            buildJsonObject {},
            buildJsonObject { put("location", "") },
            buildJsonObject { put("location", "   ") },
            buildJsonObject { put("location", 42) },
            buildJsonObject { put("location", true) },
            buildJsonObject { put("location", JsonNull) },
            buildJsonObject { put("location", buildJsonObject {}) },
            buildJsonObject { put("location", buildJsonArray {}) }
        )
        for (tool in listOf("get_current_weather", "get_weather_forecast")) {
            for (arguments in invalidArguments) {
                val result = client.callTool(tool, arguments)
                assertTrue(result.getValue("isError").jsonPrimitive.boolean)
                assertContains(result.errorText(), "non-empty location string")
            }
        }
        assertEquals(0, fixture.cacheReads)
        assertTrue(fixture.feed.requests.isEmpty())
    }

    @Test
    fun `missing weather is reported as a tool error`() = testApplication {
        configure(Fixture())
        for (tool in listOf("get_current_weather", "get_weather_forecast")) {
            val result = client.callTool(tool, buildJsonObject { put("location", "Unknown,CH") })
            assertTrue(result.getValue("isError").jsonPrimitive.boolean)
            assertContains(result.errorText(), "No weather found for Unknown,CH")
        }
    }

    @Test
    fun `provider failures return a tool error without exposing exception details`() = testApplication {
        val fixture = Fixture()
        fixture.feed.failure = IllegalStateException("private provider details")
        configure(fixture)
        for (tool in listOf("get_current_weather", "get_weather_forecast")) {
            val result = client.callTool(tool, buildJsonObject { put("location", "Unknown,CH") })
            assertTrue(result.getValue("isError").jsonPrimitive.boolean)
            assertContains(result.errorText(), "temporarily unavailable")
            assertFalse(result.toString().contains("private provider details"))
        }
    }

    @Test
    fun `existing REST routes work alongside MCP serialization`() = testApplication {
        val fixture = Fixture()
        configure(fixture)
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
        val current = client.get("/weather/current/Zurich,CH")
        assertEquals(HttpStatusCode.OK, current.status)
        assertEquals(fixture.current, Json.decodeFromString<CurrentWeather>(current.bodyAsText()))
        val forecast = client.get("/weather/forecast/Zurich,CH")
        assertEquals(HttpStatusCode.OK, forecast.status)
        assertContains(forecast.bodyAsText(), "<weatherdata>")
    }

    @Test
    fun `MCP restricts host and origin headers to configured hosts`() = testApplication {
        configure(Fixture(), allowedHosts = listOf("localhost", "weather.example.com"))
        assertEquals(HttpStatusCode.Forbidden, client.post("/mcp") {
            header(HttpHeaders.Host, "untrusted.example")
        }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/mcp") {
            header(HttpHeaders.Host, "localhost")
            header(HttpHeaders.Origin, "https://untrusted.example")
        }.status)
        val allowed = client.post("/mcp") {
            header(HttpHeaders.Host, "weather.example.com")
            header(HttpHeaders.Origin, "https://weather.example.com")
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            contentType(ContentType.Application.Json)
            setBody(rpcBody("tools/list", buildJsonObject {}))
        }
        assertEquals(HttpStatusCode.OK, allowed.status)
    }

    private fun ApplicationTestBuilder.configure(
        fixture: Fixture,
        allowedHosts: List<String> = listOf("localhost", "127.0.0.1", "[::1]")
    ) {
        application {
            install(ContentNegotiation) { json(McpJson) }
            weatherMcp(fixture.service, allowedHosts)
            routing {
                healthRoutes()
                weatherRoutes(fixture.service)
            }
        }
    }

    private suspend fun HttpClient.rpc(method: String, params: JsonObject = buildJsonObject {}): JsonObject {
        val response = post("/mcp") {
            header(HttpHeaders.Host, "localhost")
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            header("MCP-Protocol-Version", "2025-11-25")
            contentType(ContentType.Application.Json)
            setBody(rpcBody(method, params))
        }
        val body = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, body)
        return Json.parseToJsonElement(body).jsonObject.also {
            assertEquals("2.0", it.getValue("jsonrpc").jsonPrimitive.content)
            assertEquals(1, it.getValue("id").jsonPrimitive.int)
        }
    }

    private suspend fun HttpClient.callTool(name: String, arguments: JsonObject): JsonObject =
        rpc("tools/call", buildJsonObject {
            put("name", name)
            put("arguments", arguments)
        }).getValue("result").jsonObject

    private fun rpcBody(method: String, params: JsonObject) = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", 1)
        put("method", method)
        put("params", params)
    }.toString()

    private fun locationArguments() = buildJsonObject { put("location", "Zurich,CH") }

    private fun JsonObject.errorText() = getValue("content").jsonArray.first()
        .jsonObject.getValue("text").jsonPrimitive.content

    private class Fixture {
        private val now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0)
        private val currentResponse = load<CurrentWeatherResponse>("/current.json")
        val current = CurrentWeather(
            location = "Zurich,CH",
            timestamp = now,
            bootstrapIcon = "sun",
            current = requireNotNull(currentResponse.weather).first(),
            info = requireNotNull(currentResponse.main),
            sunriseSunset = requireNotNull(currentResponse.sys),
            coord = requireNotNull(currentResponse.coord)
        )
        val forecast = ForecastWeather(
            location = "Zurich,CH",
            timestamp = now,
            forecast = requireNotNull(load<ForecastWeatherResponse>("/forecast.json").list).first()
        )
        val feed = FakeFeed()
        var cacheReads = 0
        private val repo = object : WeatherRepo {
            override suspend fun getCurrent(location: String): CurrentWeather? {
                cacheReads++
                return current.takeIf { it.location == location }
            }

            override suspend fun listForecast(location: String, limit: Int): List<ForecastWeather> {
                cacheReads++
                return listOf(forecast).filter { it.location == location }.take(limit)
            }

            override suspend fun storeCurrent(weather: CurrentWeather) = Unit
            override suspend fun storeForecast(forecast: List<ForecastWeather>) = Unit
        }
        val service = WeatherService(feed, repo, MockLocationRepo())
    }

    private class FakeFeed : WeatherFeed {
        val requests = mutableListOf<String>()
        var failure: Exception? = null

        override suspend fun current(location: String): CurrentWeatherResponse? {
            requests.add(location)
            failure?.let { throw it }
            return null
        }

        override suspend fun forecast(location: String): ForecastWeatherResponse? {
            requests.add(location)
            failure?.let { throw it }
            return null
        }
    }
}
