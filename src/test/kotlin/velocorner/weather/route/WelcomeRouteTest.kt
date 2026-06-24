package velocorner.weather.route

import kotlin.test.Test
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.testing.*
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class WelcomeRouteTest {

    @Test
    fun testWelcome() = testApplication {
        routing {
            staticResources("/", "static")
            welcomeRoutes()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.contentType, response.contentType()!!.contentType)
        val text = response.bodyAsText()
        assertContains(text, "<title>Weather Service</title>")
        assertContains(text, "href=\"/welcome.css\"")
        assertContains(text, "class=\"hero\"")
        assertContains(text, "href=\"/weather/current/Zurich,CH\"")
        assertContains(text, "href=\"/weather/forecast/Zurich,CH\"")
        assertContains(text, "href=\"/docs\"")
        assertContains(text, "Build time")

        val css = client.get("/welcome.css")
        assertEquals(HttpStatusCode.OK, css.status)
        assertContains(css.bodyAsText(), ".hero")
    }
}
