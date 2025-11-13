import java.time.OffsetDateTime

val ktor_version: String by project
val kotlin_version: String by project
val plugin_version = "2.2.10"
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
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.ktor.plugin")
    id("org.graalvm.buildtools.native")
    // generate SBOM
    id("org.cyclonedx.bom")
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
    implementation(platform(libs.ktor.bom))
    implementation(Ktor.client.core)
    implementation(Ktor.client.java)
    implementation(Ktor.server.core)
    implementation(Ktor.server.netty)
    implementation(Ktor.server.contentNegotiation)
    implementation(Ktor.plugins.serialization.kotlinx.json)
    implementation(Ktor.server.htmlBuilder)
    implementation(Ktor.server.callLogging)
    implementation(Ktor.server.cors)
    implementation(libs.ktor.openapi)
    implementation(libs.ktor.swagger.ui)

    // Database dependencies
    implementation(JetBrains.exposed.core)
    implementation(JetBrains.exposed.dao)
    implementation(JetBrains.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.org.postgresql.postgresql)
    implementation(libs.ojdbc11)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.flyway.database.oracle)

    // Logging
    implementation(libs.logback.classic)

    // Test dependencies
    testImplementation(Ktor.server.testHost)
    testImplementation(Kotlin.test.junit)
    testImplementation(libs.testcontainers)
    testImplementation(libs.org.testcontainers.postgresql)
    testImplementation(libs.oracle.xe)
}

ktor {
    fatJar {
        archiveFileName = "service.jar"
    }

    // it is generated from the script, deploy.sh
    docker {
        jreVersion.set(JavaVersion.VERSION_17)
        customBaseImage = "eclipse-temurin:17-jre-noble:_"
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
