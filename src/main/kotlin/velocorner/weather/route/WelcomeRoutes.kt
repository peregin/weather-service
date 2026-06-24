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

private val welcomeLinks = listOf(
    WelcomeLink("/docs", "OpenAPI"),
    WelcomeLink("weather/current/Zurich,CH", "current weather for Zürich, Switzerland 🇨🇭"),
    WelcomeLink("weather/forecast/Zurich,CH", "5 days forecast ☀️ in 🇨🇭")
)

private data class FaviconLink(val href: String, val type: String)
private data class WelcomeLink(val href: String, val text: String)

internal fun resolveBuildTime(classLoader: ClassLoader = Thread.currentThread().contextClassLoader): String =
    classLoader.getResources("META-INF/MANIFEST.MF")
        .asSequence().firstNotNullOfOrNull { resource ->
            runCatching {
                resource.openStream().use { Manifest(it).mainAttributes.getValue("Build-Time") }
            }.getOrNull()
        }
        ?: NOT_AVAILABLE

fun Route.welcomeRoutes() {
    get("/") {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        val buildTime = resolveBuildTime()
        call.respondHtml(HttpStatusCode.OK) {
            head {
                title("Weather Service")
                faviconLinks.forEach { favicon ->
                    link(rel = "icon", href = favicon.href, type = favicon.type)
                }
            }
            body {
                h1 { +"Welcome @ $now" }
                ul {
                    welcomeLinks.forEach { welcomeLink ->
                        li { a(welcomeLink.href) { +welcomeLink.text } }
                    }
                }
                p { +"JAVA_OPTS: $javaOpts" }
                p { +"Build-Time: $buildTime" }
            }
        }
    }
}
