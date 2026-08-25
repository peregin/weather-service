# Weather Service
Provides 5 days forecast, current weather conditions and suggestions for locations.

Available on [weather.velocorner.com](https://weather.velocorner.com)


## Build
```shell
# build fat jar
./gradlew shadowJar
# build native image with gradle got GraalVM
./gradlew nativeCompile
```

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
# build with native image
docker build -t peregin/velocorner.weather:latest -f Dockerfile.graal .
docker run -it --rm --env-file ./local.env --name weather -p 9015:9015 peregin/velocorner.weather:latest
```

## Kotlin
- https://kotlinlang.org/
