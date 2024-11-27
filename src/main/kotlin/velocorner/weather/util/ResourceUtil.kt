package velocorner.weather.util

import kotlinx.serialization.json.Json

object ResourceUtil {

    val json = Json { ignoreUnknownKeys = true }

    inline fun <reified T> load(resource: String): T =
        json.decodeFromString<T>(requireNotNull(javaClass.getResource(resource)) {
            "Resource $resource not found"
        }.readText())
}