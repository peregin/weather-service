package velocorner.weather.service

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import velocorner.weather.model.CountryFeedResponse

object CountryFeed {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun country(ip: String): String {
        HttpClient(Java).use {
            val response = it.get("https://api.country.is/$ip")
            return runCatching { json.decodeFromString<CountryFeedResponse>(response.bodyAsText()) }.getOrNull()?.country
                ?: "CH"
        }
    }
}