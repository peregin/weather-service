package velocorner.weather.repo

import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.slf4j.LoggerFactory
import velocorner.weather.model.CurrentWeather
import velocorner.weather.model.ForecastWeather
import velocorner.weather.repo.DatabaseFactory.transact

interface WeatherRepo {
    // weather - location is <city[,countryISO2letter]>
    // limit for 5 days forecast broken down to 3 hours = 8 entries/day and 40 entries/5 days
    suspend fun listForecast(location: String, limit: Int = 40): List<ForecastWeather>

    suspend fun storeForecast(forecast: List<ForecastWeather>)

    suspend fun getCurrent(location: String): CurrentWeather?

    suspend fun storeCurrent(weather: CurrentWeather)
}

// quoted entity names needed by Oracle database
object CurrentWeatherTable : Table("\"weather\"") {
    val location = varchar("\"location\"", 64)
    val data = text("data") // use text for compatibility with Oracle
    override val primaryKey = PrimaryKey(location)
}

object ForecastWeatherTable : Table("\"forecast\"") {
    val location = varchar("\"location\"", 64)
    val updateTime = datetime("\"update_time\"")
    val data = text("data")
    override val primaryKey = PrimaryKey(location, updateTime)
}

class WeatherRepoImpl : WeatherRepo {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listForecast(location: String, limit: Int): List<ForecastWeather> = transact {
        ForecastWeatherTable
            .selectAll().where { ForecastWeatherTable.location eq location }
            .orderBy(Pair(ForecastWeatherTable.updateTime, SortOrder.DESC))
            .limit(limit)
            .mapNotNull { row ->
                row[ForecastWeatherTable.data].let { jsonString ->
                    runCatching { json.decodeFromString(ForecastWeather.serializer(), jsonString) }.getOrNull()
                }
            }
    }

    override suspend fun storeForecast(forecast: List<ForecastWeather>) = transact {
        forecast.forEach { w ->
            val timestamp = w.timestamp.toLocalDateTime().toKotlinLocalDateTime()
            val jsonString = json.encodeToString(ForecastWeather.serializer(), w)
            // can't use insertIgnore to have it generic with Oracle and Postgresql
            val updated = ForecastWeatherTable.update({
                (ForecastWeatherTable.location eq w.location) and
                        (ForecastWeatherTable.updateTime eq timestamp)
            }) {
                it[data] = jsonString
            }
            if (updated == 0) {
                ForecastWeatherTable.insert {
                    it[location] = w.location
                    it[updateTime] = timestamp
                    it[data] = jsonString
                }
            }
        }
    }

    override suspend fun getCurrent(location: String): CurrentWeather? = transact {
        CurrentWeatherTable
            .selectAll().where { CurrentWeatherTable.location eq location }
            .mapNotNull { row ->
                row[CurrentWeatherTable.data].let { jsonString ->
                    runCatching { json.decodeFromString(CurrentWeather.serializer(), jsonString) }.getOrNull()
                }
            }
            .singleOrNull()
    }

    override suspend fun storeCurrent(weather: CurrentWeather): Unit = transact { db ->
        logger.debug("db dialect is {}", db?.dialect)
        val jsonString = json.encodeToString(CurrentWeather.serializer(), weather)
        // can't use insertIgnore to have it generic with Oracle and Postgresql
        val updated = CurrentWeatherTable.update({ CurrentWeatherTable.location eq weather.location }) {
            it[data] = jsonString
        }
        if (updated == 0) {
            CurrentWeatherTable.insert {
                it[location] = weather.location
                it[data] = jsonString
            }
        }
    }
}
