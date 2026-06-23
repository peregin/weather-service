package velocorner.weather.repo

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.lifecycle.Startable
import org.testcontainers.utility.DockerImageName
import velocorner.weather.repo.DatabaseFactory.transact
import velocorner.weather.util.DockerUtil
import java.sql.DriverManager
import java.time.Duration

internal enum class TestDatabaseKind {
    ORACLE,
    POSTGRESQL
}

internal object TestDatabase {
    private const val POSTGRES_IMAGE = "postgres:16.4"
    private const val ORACLE_IMAGE = "container-registry.oracle.com/database/free:latest"
    private const val ORACLE_PORT = 1521
    private const val ORACLE_SERVICE = "FREEPDB1"
    private const val ORACLE_ADMIN_USER = "system"
    private const val ORACLE_ADMIN_PASSWORD = "OracleTest26Pass1"
    private const val ORACLE_DRIVER = "oracle.jdbc.OracleDriver"
    private const val POSTGRES_DRIVER = "org.postgresql.Driver"
    private const val DB_NAME = "weather_test"
    private const val DB_USER = "weather"
    private const val DB_PASSWORD = "WthrSvc26Pass1"

    private var state: TestDatabaseState? = null

    fun start(): TestDatabaseState = synchronized(this) {
        state ?: createState().also { state = it }
    }

    fun config(): Config = start().config

    suspend fun truncateWeatherTables() {
        transact { db ->
            truncateWeatherTables(db)
        }
    }

    suspend fun truncateAllTables() {
        transact { db ->
            truncateWeatherTables(db)
            LocationTable.deleteAll()
        }
    }

    suspend fun currentWeatherCount(): Long =
        transact { db ->
            when (db?.dialect) {
                is OracleDialect -> OracleCurrentWeatherTable.selectAll().count()
                is PostgreSQLDialect -> PostgresqlCurrentWeatherTable.selectAll().count()
                else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")
            }
        }

    private fun createState(): TestDatabaseState {
        System.setProperty("api.version", "1.44")

        return if (useOracle()) {
            startOracle()
        } else {
            startPostgres()
        }
    }

    private fun useOracle(): Boolean =
        System.getProperty("weather.test.database").equals("oracle", ignoreCase = true) ||
            DockerUtil.isColimaAvailable()

    private fun startPostgres(): TestDatabaseState {
        val container = PostgreSQLContainer<Nothing>(POSTGRES_IMAGE).apply {
            withDatabaseName(DB_NAME)
            withUsername(DB_USER)
            withPassword(DB_PASSWORD)
        }
        container.start()

        return TestDatabaseState(
            kind = TestDatabaseKind.POSTGRESQL,
            config = databaseConfig(POSTGRES_DRIVER, container.jdbcUrl, DB_USER, DB_PASSWORD),
            container = container
        )
    }

    private fun startOracle(): TestDatabaseState {
        val container = GenericContainer<Nothing>(DockerImageName.parse(ORACLE_IMAGE)).apply {
            withExposedPorts(ORACLE_PORT)
            withEnv("ORACLE_PWD", ORACLE_ADMIN_PASSWORD)
            withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig?.withShmSize(2L * 1024 * 1024 * 1024)
            }
            waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*\\n", 1))
            withStartupTimeout(Duration.ofMinutes(12))
        }
        container.start()

        val jdbcUrl = "jdbc:oracle:thin:@//${container.host}:${container.getMappedPort(ORACLE_PORT)}/$ORACLE_SERVICE"
        prepareOracleDatabase(jdbcUrl)

        return TestDatabaseState(
            kind = TestDatabaseKind.ORACLE,
            config = databaseConfig(ORACLE_DRIVER, jdbcUrl, DB_USER, DB_PASSWORD),
            container = container
        )
    }

    private fun truncateWeatherTables(db: org.jetbrains.exposed.v1.jdbc.Database?) {
        when (db?.dialect) {
            is OracleDialect -> {
                OracleCurrentWeatherTable.deleteAll()
                OracleForecastWeatherTable.deleteAll()
            }
            is PostgreSQLDialect -> {
                PostgresqlCurrentWeatherTable.deleteAll()
                PostgresqlForecastWeatherTable.deleteAll()
            }
            else -> throw IllegalStateException("db dialect ${db?.dialect?.name} not supported")
        }
    }

    private fun prepareOracleDatabase(jdbcUrl: String) {
        Class.forName(ORACLE_DRIVER)
        DriverManager.getConnection(jdbcUrl, ORACLE_ADMIN_USER, ORACLE_ADMIN_PASSWORD).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLESPACE "WEATHER"
                    DATAFILE 'weather_test.dbf'
                    SIZE 128M
                    AUTOEXTEND ON NEXT 64M MAXSIZE UNLIMITED
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE USER $DB_USER
                    IDENTIFIED BY "$DB_PASSWORD"
                    DEFAULT TABLESPACE "WEATHER"
                    QUOTA UNLIMITED ON "WEATHER"
                    """.trimIndent()
                )
                statement.execute("GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE PROCEDURE TO $DB_USER")
            }
        }
    }

    private fun databaseConfig(driver: String, url: String, user: String, password: String): Config =
        ConfigFactory.parseString(
            """
            db.driver="$driver"
            db.url="$url"
            db.user="$user"
            db.password="$password"
            """.trimIndent()
        )
}

internal data class TestDatabaseState(
    val kind: TestDatabaseKind,
    val config: Config,
    val container: Startable
)
