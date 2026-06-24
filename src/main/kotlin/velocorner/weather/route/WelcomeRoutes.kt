package velocorner.weather.route

import io.ktor.http.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.ktor.server.html.*
import kotlinx.html.*
import java.util.jar.Manifest

private const val NOT_AVAILABLE = "n/a"

private val javaOpts = System.getenv("JAVA_OPTS") ?: NOT_AVAILABLE

private val faviconLinks = listOf(
    FaviconLink("/favicon.ico", "image/x-icon"),
    FaviconLink("/favicon-16x16.png", "image/png"),
    FaviconLink("/favicon-32x32.png", "image/png")
)

private val actionLinks = listOf(
    ActionLink("/weather/current/Zurich,CH", "Current Weather", "Zurich, CH"),
    ActionLink("/weather/forecast/Zurich,CH", "Forecast", "5 day meteogram"),
    ActionLink("/docs", "OpenAPI", "Interactive API docs")
)

private data class FaviconLink(val href: String, val type: String)
private data class ActionLink(val href: String, val label: String, val detail: String)

internal fun resolveBuildTime(classLoader: ClassLoader = Thread.currentThread().contextClassLoader): String =
    classLoader.getResources("META-INF/MANIFEST.MF")
        .asSequence().firstNotNullOfOrNull { resource ->
            runCatching {
                resource.openStream().use { Manifest(it).mainAttributes.getValue("Build-Time") }
            }.getOrNull()
        }
        ?: NOT_AVAILABLE

private val buildTime = resolveBuildTime()

fun Route.welcomeRoutes() {
    get("/") {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        call.respondHtml(HttpStatusCode.OK) {
            attributes["lang"] = "en"
            head {
                title("Weather Service")
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                meta(
                    name = "description",
                    content = "Weather Service landing page with current weather, forecast, and OpenAPI links."
                )
                faviconLinks.forEach { favicon ->
                    link(rel = "icon", href = favicon.href, type = favicon.type)
                }
                link(rel = "stylesheet", href = "/welcome.css")
            }
            body {
                header("topbar") {
                    a(href = "/", classes = "brand") {
                        img(src = "/weatherimage.png", alt = "", classes = "brand-mark")
                        span("brand-name") { +"Weather Service" }
                    }
                    nav("nav-links") {
                        actionLinks.forEach { action ->
                            a(href = action.href) { +action.label }
                        }
                    }
                }
                main {
                    section("hero") {
                        div("hero-content") {
                            p("eyebrow") { +"Live weather endpoints" }
                            h1 { +"Weather Service" }
                            p("hero-copy") {
                                +"Current conditions, forecast data, and API documentation for location-aware clients."
                            }
                            div("hero-actions") {
                                a(href = "/weather/current/Zurich,CH", classes = "button primary") {
                                    +"Current Zurich"
                                }
                                a(href = "/docs", classes = "button secondary") {
                                    +"API Docs"
                                }
                            }
                        }
                    }
                    section("actions-band") {
                        div("actions-grid") {
                            actionLinks.forEach { action ->
                                a(href = action.href, classes = "action-card") {
                                    span("action-label") { +action.label }
                                    span("action-detail") { +action.detail }
                                }
                            }
                        }
                    }
                    section("status-band") {
                        div("status-list") {
                            div("status-item") {
                                span("status-label") { +"Local time" }
                                span("status-value") { +now }
                            }
                            div("status-item") {
                                span("status-label") { +"Build time" }
                                span("status-value") { +buildTime }
                            }
                            div("status-item") {
                                span("status-label") { +"JAVA_OPTS" }
                                span("status-value") { +javaOpts }
                            }
                        }
                    }
                }
            }
        }
    }
}
