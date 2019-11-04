import play.sbt.routes.RoutesCompiler.autoImport._
import sbtbuildinfo.BuildInfoKeys
import sbtrelease._
import ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import play.sbt.PlayImport._

val projectScalaVersion = "2.13.1"
val scalazVersion = "7.2.29"
val zioVersion = "1.0.0-RC16"
val logbackVersion = "1.2.3"
val orientDbVersion = "3.0.24"
val elasticVersion = "7.3.1"
val playWsVersion = "2.0.7" // standalone version
val playJsonVersion = "2.7.4"
val specsVersion = "4.8.0"
val mockitoVersion = "3.1.0"

val rethinkClient = "com.rethinkdb" % "rethinkdb-driver" % "2.3.3"
val mongoClient = "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0"
val orientDbClient = "com.orientechnologies" % "orientdb-client" % orientDbVersion

val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion
val playJsonJoda = "com.typesafe.play" %% "play-json-joda" % playJsonVersion
val playWsAhcStandalone = "com.typesafe.play" %% "play-ahc-ws-standalone" % playWsVersion
val playWsJsonStandalone = "com.typesafe.play" %% "play-ws-standalone-json" % playWsVersion

val apacheCommons = Seq(
  "commons-io" % "commons-io" % "2.6",
  "commons-codec" % "commons-codec" % "1.13"
)

val playTest = "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test"
val scalaSpec = "org.specs2" %% "specs2-core" % specsVersion % "test"
val scalaSpecJunit = "org.specs2" %% "specs2-junit" % specsVersion % "test"
val mockito = "org.mockito" % "mockito-core" % mockitoVersion % "test"

def logging = Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.codehaus.janino" % "janino" % "3.1.0", // conditional logback processing
  "com.papertrailapp" % "logback-syslog4j" % "1.0.0"
)
def elastic4s = Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elasticVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elasticVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elasticVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elasticVersion % "test"
)
def storage = Seq(rethinkClient, mongoClient, orientDbClient)

def scalaz = Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion
)

def zio = Seq(
  "dev.zio" %% "zio" % zioVersion,
)

lazy val runWebAppDist: ReleaseStep = ReleaseStep(
  action = { st: State =>
    val extracted = Project.extract(st)
    extracted.runAggregated(com.typesafe.sbt.packager.Keys.dist in Global in webApp, st)
  }
)

lazy val runWebAppDockerPush: ReleaseStep = ReleaseStep(
  action = { st: State =>
    val extracted = Project.extract(st)
    extracted.runAggregated(publish in Docker in webApp, st)
  }
)

lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  version := (version in ThisBuild).value,
  scalaVersion := projectScalaVersion,
  organization := "com.github.peregin",
  description := "The Cycling Platform",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions := Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked", "-encoding", "utf8"),
  scalacOptions in Test ++= Seq("-Yrangepos"),
  resolvers in ThisBuild ++= Seq(
    "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
  ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    runWebAppDist,
    runWebAppDockerPush, // will push automatically the image to the docker hub
    setNextVersion,
    commitNextVersion
    // pushChanges  // travis release script will push the changes
  )
)

lazy val dataProvider = (project in file("data-provider") withId "data-provider")
  .settings(
    buildSettings,
    name := "data-provider",
    libraryDependencies ++= Seq(
      playJson, playJsonJoda, playWsAhcStandalone,
      scalaSpec, scalaSpecJunit
    ) ++ logging
      ++ storage
      ++ apacheCommons
      ++ scalaz
      ++ zio
  )

lazy val dataSearch = (project in file("data-search") withId "data-search")
  .settings(
    buildSettings,
    name := "data-search",
    libraryDependencies ++= elastic4s
  )
  .dependsOn(dataProvider % "test->test;compile->compile")

lazy val webApp = (project in file("web-app") withId "web-app")
  .settings(
    buildSettings,
    name := "web-app",
    libraryDependencies ++= Seq(
      guice, ehcache,
      playWsJsonStandalone,
      playTest, mockito, scalaSpec
    ),
    routesGenerator := InjectedRoutesGenerator,
    BuildInfoKeys.buildInfoKeys := Seq[BuildInfoKey](
      name, version, scalaVersion, sbtVersion,
      BuildInfoKey.action("buildTime") {
        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now())
      },
      "elasticVersion" -> elasticVersion,
      "playVersion" -> play.core.PlayVersion.current,
      "scalazVersion" -> scalazVersion,
      "gitHash" -> git.gitHeadCommit.value.getOrElse("n/a")
    ),
    buildInfoPackage := "velocorner.build",
    maintainer := "velocorner.com@gmail.com",
    packageName in Docker := "velocorner.com",
    dockerExposedPorts in Docker := Seq(9000),
    dockerBaseImage in Docker := "java:8",
    dockerUsername := Some("peregin"),
    version in Docker := "latest",
    javaOptions in Universal ++= Seq("-Dplay.server.pidfile.path=/dev/null"),
    swaggerDomainNameSpaces := Seq("model"),
    swaggerPrettyJson := true,
    swaggerV3 := true
  )
  .enablePlugins(play.sbt.PlayScala, BuildInfoPlugin, com.iheart.sbtPlaySwagger.SwaggerPlugin)
  .dependsOn(dataProvider % "compile->compile; test->test")


// top level aggregate
lazy val root = (project in file(".") withId "velocorner")
  .aggregate(dataProvider, dataSearch, webApp)
  .settings(
    name := "velocorner",
    buildSettings
  )

