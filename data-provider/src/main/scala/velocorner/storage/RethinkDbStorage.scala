package velocorner.storage

import com.rethinkdb.RethinkDB
import com.rethinkdb.gen.ast.{ReqlExpr, ReqlFunction1}
import com.rethinkdb.net.{Connection, Cursor}
import com.typesafe.scalalogging.LazyLogging
import org.json.simple.JSONObject
import scalaz.Monad
import velocorner.model.strava.{Activity, Athlete, Club}
import velocorner.model.{Account, DailyProgress}
import velocorner.storage.RethinkDbStorage._
import velocorner.util.JsonIo

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

/**
  * Created by levi on 14/09/16.
  *
  * From Data Explorer:
  * <code>
  *   r.db('velocorner').table('activity');
  * </code>
  */
class RethinkDbStorage[M[_]: Monad] extends Storage[M] with LazyLogging {

  private lazy val client = RethinkDB.r
  @volatile var maybeConn: Option[Connection] = None

  // insert all activities, new ones are added, previous ones are overridden
  override def storeActivity(activities: Iterable[Activity]): M[Unit] = Monad[M].pure(activities.map { a =>
    val json = JsonIo.write(a)
    client.json(json)
  }.foreach { json =>
    // TODO: bulk store
    val result: java.util.HashMap[String, String] = client.table(ACTIVITY_TABLE).insert(json).optArg("conflict", "update").run(maybeConn)
    logger.debug(s"result $result")
  })


  override def listActivityTypes(athleteId: Long): M[Iterable[String]] = Monad[M].pure(Iterable.empty[String])

  override def dailyProgressForAthlete(athleteId: Long, activityType: String): M[Iterable[DailyProgress]] = Monad[M].pure {
    val result: Cursor[java.util.HashMap[String, String]] = client.table(ACTIVITY_TABLE).filter(reqlFunction1{ arg1 =>
      val field1 = arg1.getField("athlete").getField("id")
      val field2 = arg1.getField("type")
      field1.eq(athleteId, Nil).and(field2.eq(activityType, Nil))
    }).run(maybeConn)
    val activities = result2Activity(result.toList.asScala.toList)
    logger.debug(s"found activities ${activities.size} for $athleteId")
    DailyProgress.fromStorage(activities)
  }

  override def getActivity(id: Long): M[Option[Activity]] = Monad[M].map(getJsonById(id, ACTIVITY_TABLE))(_.map(JsonIo.read[Activity]))

  // to check how much needs to be imported from the feed
  override def listRecentActivities(athleteId: Long, limit: Int): M[Iterable[Activity]] = Monad[M].pure {
    val result: java.util.ArrayList[java.util.HashMap[String, String]] = client.table(ACTIVITY_TABLE).filter(reqlFunction1{ arg1 =>
      val field1 = arg1.getField("athlete").getField("id")
      val field2 = arg1.getField("type")
      field1.eq(athleteId, Nil).and(field2.eq("Ride", Nil))
    }).orderBy(client.desc("start_date")).limit(limit).run(maybeConn)
    val activities = result2Activity(result.asScala.toList)
    logger.debug(s"found recent activities ${activities.size} for $athleteId")
    activities
  }

  private def result2Activity(result: List[java.util.HashMap[String, String]]): Iterable[Activity] =
    result.map(JSONObject.toJSONString).map(JsonIo.read[Activity])

  private def upsert[T](jsText: T, table: String): M[Unit] = Monad[M].pure {
    val json = client.json(jsText)
    val result: java.util.HashMap[String, String] = client.table(table).insert(json).optArg("conflict", "update").run(maybeConn)
    logger.debug(s"result $result")
  }

  private def getJsonById(id: Long, table: String) = Monad[M].pure {
    val result: Cursor[java.util.HashMap[String, String]] = client.table(table).filter(reqlFunction1{ arg1 =>
      val field1 = arg1.getField("id")
      field1.eq(id)
    }).run(maybeConn)
    result.toList.asScala.toList.map(JSONObject.toJSONString).headOption
  }

  // accounts
  override def store(account: Account): M[Unit] = upsert(JsonIo.write(account), ACCOUNT_TABLE)

  override def getAccount(id: Long): M[Option[Account]] = Monad[M].map(getJsonById(id, ACCOUNT_TABLE))(_.map(JsonIo.read[Account]))

  // athletes
  override def store(athlete: Athlete): M[Unit] = upsert(JsonIo.write(athlete), ATHLETE_TABLE)

  override def getAthlete(id: Long): M[Option[Athlete]] = Monad[M].map(getJsonById(id, ATHLETE_TABLE))(_.map(JsonIo.read[Athlete]))

  // clubs
  override def store(club: Club): M[Unit] = upsert(JsonIo.write(club), CLUB_TABLE)

  override def getClub(id: Long): M[Option[Club]] = Monad[M].map(getJsonById(id, CLUB_TABLE))(_.map(JsonIo.read[Club]))

  // weather
  override def getWeatherStorage: WeatherStorage = ???

  // attributes
  override def getAttributeStorage: AttributeStorage = ???

  // various achievements
  override def getAchievementStorage: AchievementStorage = ???

  // initializes any connections, pools, resources needed to open a storage session
  override def initialize(): Unit = {
    val conn = client.connection().hostname("localhost").port(28015).connect()

    // create database if not present
    val dbNames: java.util.ArrayList[String] = client.dbList().run(conn)
    if (!dbNames.contains(DB_NAME)) client.dbCreate(DB_NAME).run(conn)

    conn.use(DB_NAME)

    // create tables if not present
    def createIfNotExists(tables: String*): Unit = {
      val tableNames: java.util.ArrayList[String] = client.tableList().run(conn)
      tables.foreach{ t =>
        if (!tableNames.contains(t)) client.tableCreate(t).run(conn)
      }
    }
    createIfNotExists(ACTIVITY_TABLE, ACCOUNT_TABLE, ATHLETE_TABLE, CLUB_TABLE)

    maybeConn = Some(conn)
    logger.info(s"connected with $conn")
  }

  // releases any connections, resources used
  override def destroy(): Unit = maybeConn.close()
}

object RethinkDbStorage {

  val DB_NAME = "velocorner"
  val ACTIVITY_TABLE = "activity"
  val ACCOUNT_TABLE = "account"
  val CLUB_TABLE = "club"
  val ATHLETE_TABLE = "athlete"

  implicit def convert(conn: Option[Connection]): Connection = conn.getOrElse(sys.error("connection is not initialized"))

  implicit def reqlFunction1(fun: ReqlExpr => Object): ReqlFunction1 = (arg1: ReqlExpr) => fun(arg1)
}
