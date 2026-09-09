# Weather Service
Provides 5 days forecast, current weather conditions and suggestions for locations.

Available on [weather.velocorner.com](https://weather.velocorner.com)


## Build

The JVM build requires a Java 21 toolchain. Native builds additionally require
a GraalVM JDK with `native-image` and the platform C toolchain (Xcode Command Line
Tools on macOS). The native executable is specific to the build OS and architecture.
Native build and smoke tests have been verified with Oracle GraalVM 25.0.4 on macOS ARM64.

```shell
# build fat jar
./gradlew shadowJar

# Build a native executable. If JAVA_HOME does not already point to GraalVM,
# set GRAALVM_HOME to a GraalVM JDK that contains native-image.
GRAALVM_HOME=/path/to/graalvm ./gradlew nativeCompile

# Run all JVM tests, then build and smoke-test the native executable.
# Requires Docker and a free local port 9015; uses a disposable database.
GRAALVM_HOME=/path/to/graalvm ./gradlew test nativeSmokeTest
```

### Run the native server

Set `WEATHER_API_KEY` to an OpenWeather API key and `DB_PASSWORD` to the database
password. `DB_DRIVER`, `DB_URL`, and `DB_USER` configure the database connection.
For example, after provisioning the local Oracle database described below:

```shell
export WEATHER_API_KEY='your-api-key'
export DB_DRIVER=oracle.jdbc.OracleDriver
export DB_URL='jdbc:oracle:thin:@//localhost:1522/FREEPDB1'
export DB_USER=weather
export DB_PASSWORD='your-database-password'
./build/native/nativeCompile/weather-service
```

The server listens on port 9015. Check `/health`, `/api.json`, and `/docs/`.
The `nativeSmokeTest` task checks fresh database migrations, static/Swagger assets,
OpenAPI schemas, location queries, and cached current/forecast weather responses.
It uses fixture data and does not call the live weather provider. Its native server
log is saved to `build/reports/tests/nativeSmokeTest/server.log`.

Native reflection and resource metadata lives in
`src/main/resources/META-INF/native-image/velocorner.weather/weather-service/`.
It retains Kotlin API model reflection, Swagger's Jackson models/mixins/serializers,
and application/Swagger resources. Update the metadata and rerun `nativeSmokeTest`
when changing API models or upgrading those libraries.

SQL migrations are indexed automatically by `generateMigrationIndex` during resource
processing. `ClasspathMigrationResourceProvider` reads that index so Flyway can load
migrations on both the JVM and Native Image without scanning embedded resource URLs.
Add new migrations under `src/main/resources/migration/{oracle,psql}/` as usual.

## Database
### PostgreSQL
```shell
./psql.sh
```
### Oracle
Run it
```shell
# Starts Oracle AI Database Free 26ai on localhost:1522.
# The script pins container-registry.oracle.com/database/free:23.26.0.0.
./osql26ai.sh
```

Connect to it with SQLDeveloper, CLI, JDBC
SQLDeveloper
26ai Free: `system` as user, `FREEPDB1` as service, `localhost:1522`

CLI
```shell
docker exec -it oracle26ai su - oracle -c "
export ORACLE_SID=orcl
export ORAENV_ASK=NO
. /usr/local/bin/oraenv
\$ORACLE_HOME/bin/sqlplus / as sysdba
"
```

```sql
-- The Oracle Database Free container already provides the FREEPDB1 pluggable database.
-- Create the application tablespace and user inside that PDB.
ALTER SESSION SET CONTAINER = FREEPDB1;

CREATE TABLESPACE "WEATHER"
  DATAFILE 'weather01.dbf'
  SIZE 256M
  AUTOEXTEND ON NEXT 128M MAXSIZE UNLIMITED;

CREATE USER weather
  IDENTIFIED BY "WthrSvc26Pass1"
  DEFAULT TABLESPACE "WEATHER"
  QUOTA UNLIMITED ON "WEATHER";

GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE PROCEDURE TO weather;
```

For Oracle Autonomous Database 26ai, connect as `ADMIN` to the Autonomous
Database and create only the application user. PDB and tablespace administration
is managed by the service.

```sql
-- Use a password that satisfies the Autonomous Database password policy.
CREATE USER weather IDENTIFIED BY "WthrSvc26Pass1" QUOTA UNLIMITED ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE PROCEDURE TO weather;
```
JDBC
```shell
# driver implemented in
oracle.jdbc.OracleDriver
# url looks like
jdbc:oracle:thin:@//localhost:1522/FREEPDB1
```

## Gradle
Useful commands and plugins
```shell
# initialize existing project with the desired wrapper version
gradle wrapper
# check for dependency updates
./gradlew build --refresh-dependencies
# upgrade gradle version
./gradlew wrapper --gradle-version 9.7.1
# generate Software Bill Of Materials SBOM
./gradlew cyclonedxBom
```

### Testcontainers with Colima

When the Gradle test task detects Colima, it uses the Colima Docker socket and
the Oracle Database container. By default it also disables Testcontainers'
Ryuk resource reaper and startup checks, avoiding their Docker Hub image pulls.
Testcontainers still cleans up normally when the test JVM exits; after a forced
JVM termination, remove any orphaned test containers manually.

Run the tests with:

```shell
./gradlew test
```

The preferred long-term setup is to publish the required helper images to a
team-owned Oracle Artifactory Docker repository that preserves Docker Hub image
paths. To enable Ryuk and the startup checks through that mirror, set either the
standard Testcontainers environment variable or the Gradle property:

```shell
TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=team-docker-local.artifactory.oci.oraclecorp.com/ ./gradlew test
./gradlew test -PtestcontainersHubImageNamePrefix=team-docker-local.artifactory.oci.oraclecorp.com/
```

Verify that the mirror contains every Docker Hub image used by the tests,
including `testcontainers/ryuk` and the Testcontainers startup-check image.
The generic `docker-remote.artifactory.oci.oraclecorp.com` proxy should only be
used after a direct pull succeeds; it may expose a manifest while failing to
serve its blobs.

## Docker
```shell
docker build -t peregin/velocorner.weather:latest .
# build ARM docker image
docker buildx build --platform linux/arm64 -t peregin/velocorner.weather:latest --push .
docker run -it --rm --env-file ./local.env --name weather -p 9015:9015 peregin/velocorner.weather:latest
```

## Kotlin
- https://kotlinlang.org/
