val ktor_version: String by project
val openapi_version: String by project
val kotlin_version: String by project
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
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
    id("io.ktor.plugin") version "3.2.3"
    id("org.cyclonedx.bom") version "1.10.0"
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
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
    testImplementation("org.testcontainers:testcontainers:$testcontainers_version")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
}

tasks {
    shadowJar {
        mergeServiceFiles()
        archiveFileName.set("service.jar")
        manifest {
            attributes(
                "Main-Class" to "velocorner.weather.ServiceKt",
                "Implementation-Version" to project.version
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
