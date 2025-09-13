FROM gradle:9-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75" TZ=UTC
RUN addgroup -S app && adduser -S app -G app
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/service.jar /app/weather-service.jar
EXPOSE 9015
USER app
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/app/weather-service.jar"]
CMD []