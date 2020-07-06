package velocorner.storage

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import velocorner.api.strava.Activity
import velocorner.model.strava.Gear
import velocorner.util.JsonIo

class PsqlDbStorageSpec extends Specification with BeforeAfterAll
  with ActivityStorageFragments with AccountStorageFragments with WeatherStorageFragments with AttributeStorageFragments
  with LazyLogging {

  sequential
  stopOnFail

  @volatile var psql: EmbeddedPostgres = _
  @volatile var psqlStorage: PsqlDbStorage = _

  "pqsl storage" should {

    val activityFixtures = JsonIo.readReadFromResource[List[Activity]]("/data/strava/last30activities.json")

    addFragmentsBlock(activityFragments(psqlStorage, activityFixtures))

    addFragmentsBlock(accountFragments(psqlStorage))

    addFragmentsBlock(weatherFragments(psqlStorage))

    addFragmentsBlock(attributeFragments(psqlStorage))

    "select achievements" in {
      val achievementStorage = psqlStorage.getAchievementStorage
      awaitOn(achievementStorage.maxAverageSpeed(432909, "Ride")).map(_.value) should beSome(7.932000160217285d)
      awaitOn(achievementStorage.maxDistance(432909, "Ride")).map(_.value) should beSome(90514.3984375d)
      awaitOn(achievementStorage.maxElevation(432909, "Ride")).map(_.value) should beSome(1077d)
      awaitOn(achievementStorage.maxHeartRate(432909, "Ride")).map(_.value) should beNone
      awaitOn(achievementStorage.maxAveragePower(432909, "Ride")).map(_.value) should beSome(233.89999389648438d)
      awaitOn(achievementStorage.minAverageTemperature(432909, "Ride")).map(_.value) should beSome(-1d)
      awaitOn(achievementStorage.maxAverageTemperature(432909, "Ride")).map(_.value) should beSome(11d)
    }

    "count entries" in {
      val adminStorage = psqlStorage.getAdminStorage
      awaitOn(adminStorage.countAccounts) === 1L
      awaitOn(adminStorage.countActivities) === activityFixtures.size
    }

    "store and lookup gears" in {
      lazy val gearStorage = psqlStorage.getGearStorage
      val gear = Gear("id1", "BMC", 12.4f)
      awaitOn(gearStorage.store(gear, Gear.Bike))
      awaitOn(gearStorage.getGear("id20")) should beNone
      awaitOn(gearStorage.getGear("id1")) should beSome(gear)
    }
  }

  override def beforeAll(): Unit = {
    logger.info("starting embedded psql...")
    try {
      psql = EmbeddedPsqlStorage()
      val port = psql.getPort
      psqlStorage = new PsqlDbStorage(dbUrl = s"jdbc:postgresql://localhost:$port/postgres", dbUser = "postgres", dbPassword = "test")
      psqlStorage.initialize()
    } catch {
      case any: Exception =>
        logger.error("failed to start embedded psql", any)
    }
  }

  override def afterAll(): Unit = {
    psqlStorage.destroy()
    psql.close()
  }
}
