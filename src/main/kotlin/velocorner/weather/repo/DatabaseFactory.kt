package velocorner.weather.repo

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    // if not found return null and chain it at the caller with the Elvis operator
    private fun Config.tryString(path: String): String? = if (this.hasPath(path)) this.getString(path) else null

    fun init(driverClassName: String = "org.postgresql.Driver", config: Config? = null) {
        val dbUrl = config?.tryString("db.url") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5494/weather"
        val dbUser = config?.tryString("db.user") ?: System.getenv("DB_USER") ?: "weather"
        val dbPassword = config?.tryString("db.password") ?: System.getenv("DB_PASSWORD")
        val dataSource = hikari(dbUrl, dbUser, dbPassword, driverClassName)
        Database.connect(dataSource)
        val flyway = Flyway.configure().locations("psql/migration").validateMigrationNaming(false).dataSource(dataSource).load()
        flyway.migrate()
    }

    private fun hikari(dbUrl: String, dbUser: String, dbPassword: String, driverClassName: String): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = driverClassName
        config.jdbcUrl = dbUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 3
        config.minimumIdle = 1
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> transact(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}
