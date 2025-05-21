package velocorner.weather.repo

import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.vendors.OracleDialect
import org.jetbrains.exposed.sql.vendors.PostgreSQLDialect
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
abstract class CurrentWeatherTable : Table("\"weather\"") {
    val location = varchar("\"location\"", 64)
    override val primaryKey = PrimaryKey(location)
}

object OracleCurrentWeatherTable : CurrentWeatherTable() {
    val data = text("data") // use text for compatibility with Oracle - CLOB
}

object PostgresqlCurrentWeatherTable : CurrentWeatherTable() {
    val data = json<CurrentWeather>("data", Json) // use json with Psql - jsonb
}

abstract class ForecastWeatherTable : Table("\"forecast\"") {
    val location = varchar("\"location\"", 64)
    val updateTime = datetime("\"update_time\"")
    override val primaryKey = PrimaryKey(location, updateTime)
}

object OracleForecastWeatherTable : ForecastWeatherTable() {
    val data = text("data") // use text for compatibility with Oracle - CLOB
}

object PostgresqlForecastWeatherTable : ForecastWeatherTable() {
    val data = json<ForecastWeather>("data", Json) // use json with Psql - jsonb
}

class WeatherRepoImpl : WeatherRepo {

    //private val logger = LoggerFactory.getLogger(this.javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listForecast(location: String, limit: Int): List<ForecastWeather> = transact { db ->
        when (db?.dialect) {
            is OracleDialect -> OracleForecastWeatherTable
                .selectAll().where { OracleForecastWeatherTable.location eq location }
                .orderBy(Pair(OracleForecastWeatherTable.updateTime, SortOrder.DESC))
                .limit(limit)
                .mapNotNull {
                    it[OracleForecastWeatherTable.data].let { jsonString ->
                        runCatching { json.decodeFromString(ForecastWeather.serializer(), jsonString) }.getOrNull()
                    }
                }

            is PostgreSQLDialect -> PostgresqlForecastWeatherTable
                .selectAll().where { PostgresqlForecastWeatherTable.location eq location }
                .orderBy(Pair(PostgresqlForecastWeatherTable.updateTime, SortOrder.DESC))
                .limit(limit)
                .mapNotNull { it[PostgresqlForecastWeatherTable.data] }

            else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")
        }
    }

    override suspend fun storeForecast(forecast: List<ForecastWeather>) = transact { db ->
        forecast.forEach { w ->
            when (db?.dialect) {
                is OracleDialect -> {
                    val timestamp = w.timestamp.toLocalDateTime().toKotlinLocalDateTime()
                    val jsonString = json.encodeToString(ForecastWeather.serializer(), w)
                    // can't use insertIgnore to have it generic with Oracle and Postgresql
                    val updated = OracleForecastWeatherTable.update({
                        (OracleForecastWeatherTable.location eq w.location) and
                                (OracleForecastWeatherTable.updateTime eq timestamp)
                    }) {
                        it[data] = jsonString
                    }
                    if (updated == 0) {
                        OracleForecastWeatherTable.insert {
                            it[location] = w.location
                            it[updateTime] = timestamp
                            it[data] = jsonString
                        }
                    }
                }

                is PostgreSQLDialect -> PostgresqlForecastWeatherTable.insertIgnore {
                    it[location] = w.location
                    it[updateTime] = w.timestamp.toLocalDateTime().toKotlinLocalDateTime()
                    it[data] = w
                }

                else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")
            }
        }
    }

    override suspend fun getCurrent(location: String): CurrentWeather? = transact { db ->
        when (db?.dialect) {
            is OracleDialect -> OracleCurrentWeatherTable.selectAll()
                .where { OracleCurrentWeatherTable.location eq location }
                .mapNotNull { row ->
                    row[OracleCurrentWeatherTable.data].let { jsonString ->
                        runCatching { json.decodeFromString(CurrentWeather.serializer(), jsonString) }.getOrNull()
                    }
                }
                .singleOrNull()

            is PostgreSQLDialect -> PostgresqlCurrentWeatherTable.selectAll()
                .where { PostgresqlCurrentWeatherTable.location eq location }
                .mapNotNull { it[PostgresqlCurrentWeatherTable.data] }
                .singleOrNull()

            else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")
        }
    }

    override suspend fun storeCurrent(weather: CurrentWeather): Unit = transact { db ->
        when (db?.dialect) {
            is OracleDialect -> {
                val jsonString = json.encodeToString(CurrentWeather.serializer(), weather)
                val updated =
                    OracleCurrentWeatherTable.update({ OracleCurrentWeatherTable.location eq weather.location }) {
                        it[data] = jsonString
                    }
                if (updated == 0) {
                    OracleCurrentWeatherTable.insert {
                        it[location] = weather.location
                        it[data] = jsonString
                    }
                }
            }

            is PostgreSQLDialect -> {
                PostgresqlCurrentWeatherTable.insertIgnore {
                    it[location] = weather.location
                    it[data] = weather
                }
            }

            else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")

        }
    }
}
