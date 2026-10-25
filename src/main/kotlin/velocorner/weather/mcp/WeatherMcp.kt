package velocorner.weather.mcp

import io.ktor.server.application.Application
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStatelessStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import velocorner.weather.service.WeatherService
import velocorner.weather.util.CountryUtil

private val logger = LoggerFactory.getLogger("WeatherMcp")

fun Application.weatherMcp(
    service: WeatherService,
    allowedHosts: List<String> = listOf("localhost", "127.0.0.1", "[::1]")
) {
    mcpStatelessStreamableHttp(
        path = "/mcp",
        allowedHosts = allowedHosts,
        // The SDK expects URLs here and compares their hostnames independently of scheme/port.
        allowedOrigins = allowedHosts.map { "https://$it" }
    ) {
        // Stateless transports close their server after each request; share only the service.
        Server(
            serverInfo = Implementation(name = "weather-service", version = "1.0.0"),
            options = ServerOptions(
                capabilities = ServerCapabilities(tools = ServerCapabilities.Tools())
            )
        ).apply {
            weatherTool(
                name = "get_current_weather",
                description = "Get current weather for a city. Temperatures are Celsius, " +
                    "humidity is percent, pressure is hPa, and timestamps are Unix seconds."
            ) { location ->
                service.current(location)?.let { Json.encodeToJsonElement(it).jsonObject }
            }
            weatherTool(
                name = "get_weather_forecast",
                description = "Get the five-day weather forecast in three-hour intervals for a city. " +
                    "Temperatures are Celsius, wind speed is m/s, precipitation is mm per three hours, " +
                    "humidity and cloud cover are percent, pressure is hPa, and timestamps are Unix seconds."
            ) { location ->
                service.forecast(location).takeIf { it.isNotEmpty() }?.let { forecast ->
                    buildJsonObject {
                        put("forecast", Json.encodeToJsonElement(forecast))
                    }
                }
            }
        }
    }
}

private fun Server.weatherTool(
    name: String,
    description: String,
    load: suspend (String) -> JsonObject?
) {
    addTool(
        name = name,
        description = description,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("location", buildJsonObject {
                    put("type", "string")
                    put("description", "City and optional country code or name, e.g. Zurich,CH")
                    put("minLength", 1)
                })
            },
            required = listOf("location")
        ),
        toolAnnotations = ToolAnnotations(
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = true
        )
    ) { request ->
        val argument = request.arguments?.get("location") as? JsonPrimitive
        val location = argument?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@addTool toolError("Provide a non-empty location string, e.g. Zurich,CH.")

        try {
            val result = load(CountryUtil.iso(location))
                ?: return@addTool toolError("No weather found for $location.")
            CallToolResult(
                content = listOf(TextContent(result.toString())),
                structuredContent = result,
                isError = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("MCP tool {} failed", name, e)
            toolError("Weather is temporarily unavailable. Try again later.")
        }
    }
}

private fun toolError(message: String) = CallToolResult(
    content = listOf(TextContent(message)),
    isError = true
)
