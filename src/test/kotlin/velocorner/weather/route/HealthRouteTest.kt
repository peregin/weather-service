package velocorner.weather.route

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HealthRouteTest {

    @Test
    fun testHealth() = testApplication {
        routing {
            healthRoutes()
        }

        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }
}
