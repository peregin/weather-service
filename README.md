# Weather Service
Provides 5 days forecast, current weather conditions and suggestions for locations.

Available on [weather.velocorner.com](https://weather.velocorner.com)

## TODO
- optimize CI/CD, add test steps and deploy.sh should just copy the already built jar file

## Deploy
```shell
./gradlew shadowJar
docker buildx build --platform linux/arm64 -t peregin/velocorner.weather:latest --push .
```

## Database
### PostgreSQL
```shell
./psql.sh
```
### Oracle
Installation instructions
https://dev.to/udara_dananjaya/running-oracle-19c-database-with-docker-1akg

Run it
```shell
# PDBADMIN, SYSTEM, SYS users
./osql19ee.sh
# or 
./osql23ai.sh
```

Connect to it with SQLDeveloper, CLI, JDBC
SQLDeveloper
19: system as user, orapdb1 as service
23: sys as user, free as service

CLI
docker exec -it oracle23ai su - oracle -c "
export ORACLE_SID=orcl
export ORAENV_ASK=NO
. /usr/local/bin/oraenv
\$ORACLE_HOME/bin/sqlplus / as sysdba
"

```shell
-- move to the root container 
ALTER SESSION SET CONTAINER = CDB$ROOT;

-- create pluggable database
CREATE PLUGGABLE DATABASE weather ADMIN USER pdbadmin IDENTIFIED BY password ROLE=(DBA) DEFAULT TABLESPACE weather DATAFILE SIZE 256M AUTOEXTEND ON NEXT 128M MAXSIZE UNLIMITED;
ALTER PLUGGABLE DATABASE weather OPEN;
ALTER PLUGGABLE DATABASE weather SAVE STATE;
ALTER SESSION SET CONTAINER = weather;
-- create dedicated user
CREATE USER weather IDENTIFIED BY weather DEFAULT TABLESPACE weather;
-- Grant basic privileges to the user
GRANT CONNECT, RESOURCE TO weather;
-- Optionally, grant additional permissions
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE PROCEDURE TO weather;
-- Optionally, set quota on the user's default tablespace
ALTER USER weather QUOTA UNLIMITED ON weather;
```
JDBC
```shell
# driver implemented in
oracle.jdbc.driver.OracleDriver
# url looks like
jdbc:oracle:thin:@//localhost:1521/weather
```

## Gradle
Useful commands and plugins
```shell
# initialize existing project with the desired wrapper version
gradle wrapper
# check for dependency updates
./gradlew checkUpdates
# upgrade gradle version
./gradlew wrapper --gradle-version 8.12.1
# generate Software Bill Of Materials SBOM
./gradlew cyclonedxBom
```

## Docker
```shell
docker build -t peregin/velocorner.weather:latest .
docker build -t peregin/velocorner.weather:latest -f Dockerfile.graal .
docker run -it --rm --env-file ./local.env --name weather -p 9015:9015 peregin/velocorner.weather:latest
```

## Kotlin
- https://kotlinlang.org/