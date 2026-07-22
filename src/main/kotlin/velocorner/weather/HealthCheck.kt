package velocorner.weather

import java.net.HttpURLConnection
import java.net.URI
import kotlin.system.exitProcess

object HealthCheck {
    @JvmStatic
    fun main(args: Array<String>) {
        val connection = URI("http://localhost:9015/health").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                exitProcess(1)
            }
        } catch (_: Exception) {
            exitProcess(1)
        } finally {
            connection.disconnect()
        }
    }
}
