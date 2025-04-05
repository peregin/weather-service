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
docker run --name "oracle19.3" -p 1521:1521 -p 5500:5500 \  
    -e ORACLE_PDB=orapdb1 \  
    -e ORACLE_PWD=password \  
    -e ORACLE_MEM=3000 \  
    -v /opt/oracle/oradata:/opt/oracle/oradata \  
    -d oracle/database:19.3.0-ee 
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
docker run -it --rm --env-file ./local.env --name weather -p 9015:9015 peregin/velocorner.weather:latest
```

## Kotlin
- https://kotlinlang.org/