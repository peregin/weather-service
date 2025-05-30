package velocorner.weather.repo

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

data class DatabaseSpecific(
    val transactionIsolation: String,
    val flywayLocation: String
)

object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    // snippet to identify in the jdbc url
    private const val oracleSnippet = ":oracle:"

    // if not found return null and chain it at the caller with the Elvis operator
    private fun Config.tryString(path: String): String? = if (this.hasPath(path)) this.getString(path) else null

    // only password is mandatory to have it configured
    // it defaults to PSQL driver and db, but support Oracle autonomous database as well
    fun init(config: Config? = null) {
        val driverClassName = config?.tryString("db.driver") ?: System.getenv("DB_DRIVER") ?: "org.postgresql.Driver"
        val dbUrl = config?.tryString("db.url") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5494/weather"
        logger.info("connecting to $dbUrl")
        val dbUser = config?.tryString("db.user") ?: System.getenv("DB_USER") ?: "weather"
        logger.info("connecting as $dbUser")
        val dbPassword = config?.tryString("db.password") ?: System.getenv("DB_PASSWORD")
        requireNotNull(dbPassword) { "DB_PASSWORD is required" }

        val specific = when {
            dbUrl.contains(oracleSnippet) -> DatabaseSpecific("2", "migration/oracle") // SERIALIZABLE
            else -> DatabaseSpecific("TRANSACTION_REPEATABLE_READ", "migration/psql")
        }
        logger.info("creating datasource with $specific")
        val dataSource = hikari(dbUrl, dbUser, dbPassword, driverClassName, specific)
        Database.connect(dataSource)
        val flyway = Flyway.configure()
            .locations(specific.flywayLocation)
            .validateMigrationNaming(false)
            .dataSource(dataSource)
            .load()
        flyway.migrate()
    }

    private fun hikari(
        dbUrl: String,
        dbUser: String,
        dbPassword: String,
        driverClassName: String,
        specific: DatabaseSpecific
    ): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = driverClassName
        config.jdbcUrl = dbUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 3
        config.minimumIdle = 1
        config.isAutoCommit = false
        config.transactionIsolation = specific.transactionIsolation
        config.validate()
        println(config.toString())
        println("driver: ${config.driverClassName}")
        println("url: ${config.jdbcUrl}")
        println("user: ${config.username}")
        println("specific: ${specific}")
        return HikariDataSource(config)
    }

    suspend fun <T> transact(block: (db: Database?) -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block(db) }
        }
}
