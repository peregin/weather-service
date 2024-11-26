# Weather Service
Provides 5 days forecast, current weather conditions and suggestions for locations.

## TODO
- optimize CI/CD, add test steps and deploy.sh should just copy the already built jar file

## Deploy
```shell
./gradlew shadowJar
docker buildx build --platform linux/arm64 -t peregin/velocorner.weather:latest --push .
```

## Gradle
Useful commands and plugins
```shell
# initialize existing project with the desired wrapper version
gradle wrapper
# check for dependency updates
./gradlew checkUpdates
# upgrade gradle version
./gradlew wrapper --gradle-version 8.11.1
```

## Docker
```shell
docker run -it --rm --name weather -p 9015:9015 peregin/velocorner.weather:latest
```

## Kotlin
- https://kotlinlang.org/