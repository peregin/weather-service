object Dependencies {

  val projectScalaVersion = "2.13.10"

  val flywayVersion = "9.7.0"
  val catsVersion = "2.8.0"
  val catsEffectVersion = "3.3.14"
  val mouseVersion = "1.2.0"
  val fs2Version = "3.3.0"
  val zioVersion = "2.0.3"
  val http4s = "0.23.16"
  val finatraVersion = "21.2.0"
  val argonautVersion = "6.3.8"
  val playWsVersion = "2.1.10" // standalone version
  val playJsonVersion = "2.9.3"
  val shapelessVersion = "2.3.10"
  val logbackVersion = "1.4.4"
  val doobieVersion = "1.0.0-RC2"
  val orientDbVersion = "3.2.11"
  val mongoDbVersion = "4.7.2"
  val rethinkDbVersion = "2.4.4"
  val elasticVersion = "8.4.3"
  val jwtVersion = "9.1.1"
  val squantsVersion = "1.8.3"
  val sparkVersion = "3.3.1"
  val circeVersion = "0.14.3"
  val scalacacheVersion = "0.28.0"
  val jsoupVersion = "1.15.3"
  val scalaTestVersion = "3.2.14"
  val mockitoVersion = "4.8.1"

  val springVersion = "2.7.5" // java world
}

object DockerBuild {

  val baseImage = "openjdk:11-jre-slim-buster"
  val maintainer = "velocorner.com@gmail.com"
}
