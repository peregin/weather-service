import java.time.OffsetDateTime

val ktor_version: String by project
val kotlin_version: String by project
val plugin_version = "2.1.20"
val openapi_version: String by project
val logback_version: String by project
val exposed_version: String by project
val flyway_version: String by project
val hikari_version: String by project
val psql_version: String by project
val oracle_version: String by project
val testcontainers_version: String by project

group = "velocorner.weather"
version = "1.0.1-SNAPSHOT"

plugins {
    application
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
    id("name.remal.check-updates") version "1.5.0"
    id("io.ktor.plugin") version "3.1.2"
    id("org.graalvm.buildtools.native") version "0.10.6"
    // generate SBOM
    id("org.cyclonedx.bom") version "2.2.0"
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("weather-service")
            buildArgs.add("--enable-url-protocols=http,https")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            // Add if you're using reflection
            buildArgs.add("--initialize-at-build-time=kotlin")
            buildArgs.add("-H:+AddAllCharsets")
        }
    }
}


repositories {
    mavenCentral()
}

application {
    mainClass.set("velocorner.weather.ServiceKt")
}

dependencies {
    // Ktor dependencies
    implementation(platform("io.ktor:ktor-bom:$ktor_version"))
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-java:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.github.smiley4:ktor-openapi:$openapi_version")
    implementation("io.github.smiley4:ktor-swagger-ui:$openapi_version")

    // Database dependencies
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-json:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("org.postgresql:postgresql:$psql_version")
    implementation("com.oracle.database.jdbc:ojdbc11:$oracle_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Test dependencies
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$plugin_version")
    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    // testImplementation("org.testcontainers:junit-jupiter:$testcontainers_version")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
}

ktor {
    fatJar {
        archiveFileName = "service.jar"
    }

    // it is generated from the script, deploy.sh
    docker {
        jreVersion.set(JavaVersion.VERSION_17)
        customBaseImage = "openjdk:17-slim-buster"
        localImageName.set("velocorner.weather")
        imageTag.set("latest")
        portMappings.set(
            listOf(
                io.ktor.plugin.features.DockerPortMapping(
                    9015,
                    9015,
                    io.ktor.plugin.features.DockerPortMappingProtocol.TCP
                )
            )
        )

        externalRegistry.set(
            io.ktor.plugin.features.DockerImageRegistry.dockerHub(
                appName = provider { "velocorner.weather" },
                username = provider { "peregin" },
                password = providers.environmentVariable("DOCKER_HUB_PASSWORD")
            )
        )
    }
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        mergeServiceFiles()
        archiveBaseName.set("WeatherApp")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "velocorner.weather.ServiceKt",
                    "Build-Time" to OffsetDateTime.now().toString(),
                    "Implementation-Version" to project.version
                )
            )
        }
    }

    cyclonedxBom {
        setIncludeConfigs(listOf("runtimeClasspath"))
        setDestination(layout.buildDirectory.dir("reports/cyclonedx").get().asFile)
        setOutputName(project.name + ".cdx.sbom")
        outputFormat.set("json")
    }
}
