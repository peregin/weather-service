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

    // Helper: Run block for dialect with tables
    private inline fun <T> withDialect(db: Database?, oracle: () -> T, postgres: () -> T): T =
        when (db?.dialect) {
            is OracleDialect -> oracle()
            is PostgreSQLDialect -> postgres()
            else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")
        }

    override suspend fun listForecast(location: String, limit: Int): List<ForecastWeather> = transact { db ->
        withDialect(
            db,
            oracle = {
                OracleForecastWeatherTable
                    .selectAll().where { OracleForecastWeatherTable.location eq location }
                    .orderBy(OracleForecastWeatherTable.updateTime to SortOrder.DESC)
                    .limit(limit)
                    .mapNotNull { row ->
                        runCatching {
                            json.decodeFromString(ForecastWeather.serializer(), row[OracleForecastWeatherTable.data])
                        }.getOrNull()
                    }
            },
            postgres = {
                PostgresqlForecastWeatherTable
                    .selectAll().where { PostgresqlForecastWeatherTable.location eq location }
                    .orderBy(PostgresqlForecastWeatherTable.updateTime to SortOrder.DESC)
                    .limit(limit)
                    .mapNotNull { it[PostgresqlForecastWeatherTable.data] }
            }
        )
    }

    override suspend fun storeForecast(forecast: List<ForecastWeather>): Unit = transact { db ->
        withDialect(
            db,
            oracle = {
                forecast.forEach { w ->
                    val timestamp = w.timestamp.toLocalDateTime().toKotlinLocalDateTime()
                    val jsonString = json.encodeToString(ForecastWeather.serializer(), w)
                    val updated = OracleForecastWeatherTable.update(
                        { (OracleForecastWeatherTable.location eq w.location) and (OracleForecastWeatherTable.updateTime eq timestamp) }
                    ) { it[data] = jsonString }
                    if (updated == 0) {
                        OracleForecastWeatherTable.insert {
                            it[location] = w.location
                            it[updateTime] = timestamp
                            it[data] = jsonString
                        }
                    }
                }
            },
            postgres = {
                // Batch insert (better performance)
                PostgresqlForecastWeatherTable.batchInsert(forecast, ignore = true) { w ->
                    this[PostgresqlForecastWeatherTable.location] = w.location
                    this[PostgresqlForecastWeatherTable.updateTime] =
                        w.timestamp.toLocalDateTime().toKotlinLocalDateTime()
                    this[PostgresqlForecastWeatherTable.data] = w
                }
            }
        )
    }

    override suspend fun getCurrent(location: String): CurrentWeather? = transact { db ->
        withDialect(
            db,
            oracle = {
                OracleCurrentWeatherTable
                    .selectAll().where { OracleCurrentWeatherTable.location eq location }
                    .mapNotNull { row ->
                        runCatching {
                            json.decodeFromString(CurrentWeather.serializer(), row[OracleCurrentWeatherTable.data])
                        }.getOrNull()
                    }
                    .singleOrNull()
            },
            postgres = {
                PostgresqlCurrentWeatherTable
                    .selectAll().where { PostgresqlCurrentWeatherTable.location eq location }
                    .mapNotNull { it[PostgresqlCurrentWeatherTable.data] }
                    .singleOrNull()
            }
        )
    }

    override suspend fun storeCurrent(weather: CurrentWeather): Unit = transact { db ->
        withDialect(
            db,
            oracle = {
                val jsonString = json.encodeToString(CurrentWeather.serializer(), weather)
                val updated = OracleCurrentWeatherTable.update(
                    { OracleCurrentWeatherTable.location eq weather.location }
                ) { it[data] = jsonString }
                if (updated == 0) {
                    OracleCurrentWeatherTable.insert {
                        it[location] = weather.location
                        it[data] = jsonString
                    }
                }
            },
            postgres = {
                PostgresqlCurrentWeatherTable.insertIgnore {
                    it[location] = weather.location
                    it[data] = weather
                }
            }
        )
    }
}
