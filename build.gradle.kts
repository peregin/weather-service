import java.time.Instant

val ktor_version = project.property("ktor_version") as String
val openapi_version = project.property("openapi_version") as String
val kotlin_version = project.property("kotlin_version") as String
val logback_version = project.property("logback_version") as String
val exposed_version = project.property("exposed_version") as String
val flyway_version = project.property("flyway_version") as String
val hikari_version = project.property("hikari_version") as String
val psql_version = project.property("psql_version") as String
val oracle_version = project.property("oracle_version") as String
val testcontainers_version = project.property("testcontainers_version") as String

group = "velocorner.weather"
version = "1.0.1-SNAPSHOT"

plugins {
    application
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
    id("io.ktor.plugin") version "3.5.1"
    id("org.cyclonedx.bom") version "3.3.0"
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("velocorner.weather.ServiceKt")
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-java:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.github.smiley4:ktor-openapi:${openapi_version}")
    implementation("io.github.smiley4:ktor-swagger-ui:${openapi_version}")

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
    implementation("org.flywaydb:flyway-database-oracle:$flyway_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainers_version")
}

tasks {
    shadowJar {
        // Keep duplicate service descriptors so ServiceFileTransformer can merge them.
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        archiveFileName.set("service.jar")
        manifest {
            attributes(
                "Main-Class" to "velocorner.weather.ServiceKt",
                "Implementation-Version" to project.version,
                "Build-Time" to Instant.now().toString()
            )
        }
    }

    withType<Test>().configureEach {
        doFirst {
            val colimaSocket = listOf(
                file("${System.getProperty("user.home")}/.colima/default/docker.sock"),
                file("${System.getProperty("user.home")}/.colima/docker.sock")
            ).firstOrNull { it.exists() }

            if (colimaSocket != null) {
                val dockerHost = "unix://${colimaSocket.absolutePath}"

                environment("DOCKER_HOST", dockerHost)
                environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
                systemProperty(
                    "docker.client.strategy",
                    "org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy"
                )
                systemProperty("docker.host", dockerHost)
                systemProperty("weather.test.database", "oracle")

                logger.lifecycle("Using Colima Docker socket for tests: $dockerHost")
                logger.lifecycle("Using Oracle database container for tests because Colima was detected")
            }
        }
    }
}
