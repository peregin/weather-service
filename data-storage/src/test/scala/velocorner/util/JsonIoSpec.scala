package velocorner.util

import org.specs2.mutable.Specification
import velocorner.model.{Athlete, Activity}

import scala.io.Source

/**
 * Created by levi on 08/02/15.
 */
class JsonIoSpec extends Specification {

  addArguments(stopOnFail)

  "converter for athlete activities" should {

    val json = Source.fromURL(getClass.getResource("/data/strava/last10activities.json")).mkString

    "read Strava activity" in {
      val list = JsonIo.read[List[Activity]](json)
      list must haveSize(30)

      val first = list.head
      first.name === "Stallikon Ride"
      first.distance === 23216.8f
    }

    "read and write" in {
      val list = JsonIo.read[List[Activity]](json)
      val activity = list.head

      val jsonText = JsonIo.write(activity)
      val otherActivity = JsonIo.read[Activity](jsonText)
      activity === otherActivity
    }
  }

  "converter for club activities" should {
    val json = Source.fromURL(getClass.getResource("/data/strava/club_activity.json")).mkString

    "read the json file" in {
      val list = JsonIo.read[List[Activity]](json)
      list must haveSize(1)
    }
  }

  "writer for athlete" should {
    "generate type field" in {
      val a = Athlete(1, 2, Some("Levi"), None)
      val json = JsonIo.write(a)
      json must contain("type\" : \"Athlete")
    }
  }

}
