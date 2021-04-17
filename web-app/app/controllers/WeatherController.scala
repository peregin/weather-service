package controllers

import controllers.util.WebMetrics

import javax.inject.Inject
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, Duration, LocalDate}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import velocorner.api.weather.{CurrentWeather, DailyWeather, WeatherDescription, WeatherForecast, WeatherInfo}
import velocorner.model._
import velocorner.util.{CountryUtils, JsonIo, WeatherCodeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.data.{EitherT, OptionT}
import cats.implicits._
import cats.instances.future.catsStdInstancesForFuture
import model.highcharts
import play.api.libs.json.Json
import velocorner.api.GeoPosition
import velocorner.storage.PsqlDbStorage
import velocorner.util.CountryUtils.normalize

class WeatherController @Inject() (val connectivity: ConnectivitySettings, components: ControllerComponents)
    extends AbstractController(components)
    with WebMetrics {

  // keeps the weather data so much time in the cache
  val refreshTimeoutInMinutes = 15

  def clock() = DateTime.now() // inject time iterator instead

  // retrieves the weather forecast for a given place
  // route mapped to /api/weather/forecast/:location
  def forecast(location: String) = Action.async {
    timedRequest(s"query weather forecast for $location") { implicit request =>
      // convert city[,country] to city[,isoCountry]
      val isoLocation = CountryUtils.iso(location)
      val now = clock()
      val weatherStorage = connectivity.getStorage.getWeatherStorage
      val attributeStorage = connectivity.getStorage.getAttributeStorage // for storing the last update
      logger.debug(s"collecting weather forecast for [$location] -> [$isoLocation] at $now")

      // if not in storage use a one year old ts to trigger the query
      def lastUpdateTime: Future[DateTime] = attributeStorage.getAttribute(isoLocation, attributeStorage.forecastTsKey).map {
        case Some(timeText) => DateTime.parse(timeText, DateTimeFormat.forPattern(DateTimePattern.longFormat))
        case _              => now.minusYears(1)
      }

      def retrieveAndStore(place: String): Future[List[WeatherForecast]] = for {
        entries <- connectivity.getWeatherFeed.forecast(place).map(res => res.points.map(w => WeatherForecast(place, w.dt.getMillis, w)))
        _ = logger.info(s"querying latest weather forecast for $place")
        _ <- weatherStorage.storeRecentForecast(entries)
        _ <- attributeStorage.storeAttribute(place, attributeStorage.forecastTsKey, now.toString(DateTimePattern.longFormat))
      } yield entries

      val resultET = for {
        place <- EitherT(Future(Option(isoLocation).filter(_.nonEmpty).toRight(BadRequest)))
        lastUpdate <- EitherT.right[Status](lastUpdateTime)
        elapsedInMinutes = new Duration(lastUpdate, now).getStandardMinutes // should be more than configurable minutes to execute the query
        _ = logger.info(s"last weather update on $place was at $lastUpdate, $elapsedInMinutes minutes ago")
        entries <- EitherT.right[Status](
          if (elapsedInMinutes > refreshTimeoutInMinutes) retrieveAndStore(place) else weatherStorage.listRecentForecast(place)
        )
      } yield entries

      // generate json or xml content
      type transform2Content = List[WeatherForecast] => String
      val contentGenerator: transform2Content = request.getQueryString("mode") match {
        case Some("xml") => wt => highcharts.toMeteoGramXml(wt).toString()
        case _           => wt => JsonIo.write(DailyWeather.list(wt))
      }

      resultET
        .map(_.toList.sortBy(_.timestamp))
        .map(contentGenerator)
        .map(Ok(_).withCookies(WeatherCookie.create(location)))
        .merge
    }
  }

  // retrieves the sunrise and sunset information for a given place
  // route mapped to /api/weather/current/:location
  def current(location: String) = Action.async {
    timedRequest(s"query sunrise sunset for $location") { implicit request =>
      // convert city[,country] to city[,isoCountry]
      val isoLocation = CountryUtils.iso(location)
      val now = clock()
      val weatherStorage = connectivity.getStorage.getWeatherStorage
      val attributeStorage = connectivity.getStorage.getAttributeStorage // for storing the last update
      logger.debug(s"collecting current weather for [$location] -> [$isoLocation] at [$now]")

      // if not in storage use a one year old ts to trigger the query
      def lastUpdateTime: Future[DateTime] = attributeStorage.getAttribute(isoLocation, attributeStorage.weatherTsKey).map {
        case Some(timeText) => DateTime.parse(timeText, DateTimeFormat.forPattern(DateTimePattern.longFormat))
        case _              => now.minusYears(1)
      }

      // store sunrise/sunset and location geo position
      def retrieveAndStore: OptionT[Future, CurrentWeather] = for {
        response <- OptionT(connectivity.getWeatherFeed.current(isoLocation))
        // weather forecast
        currentWeather <- OptionT(Future(for {
          current <- response.weather.getOrElse(Nil).headOption
          info <- response.main
          sunset <-response.sys
        } yield CurrentWeather(
          location = isoLocation, // city[, country iso 2 letters]
          timestamp = now,
          bootstrapIcon = WeatherCodeUtils.bootstrapIcon(current.id),
          current = current,
          info = info,
          sunriseSunset = sunset
        )))
        _ <- OptionT.liftF(weatherStorage.storeRecentWeather(currentWeather))
        // geo location based on place definition used for wind status
        newGeoLocation <- OptionT(Future(response.coord.map(s => GeoPosition(latitude = s.lat, longitude = s.lon))))
        _ <- OptionT.liftF(connectivity.getStorage.getLocationStorage.store(isoLocation, newGeoLocation))
      } yield currentWeather

      val resultET = for {
        place <- EitherT(Future(Option(isoLocation).filter(_.nonEmpty).toRight(BadRequest)))
        // it is in the storage or retrieve it and store it
        currentWeather <- OptionT(weatherStorage.getRecentWeather(place)).orElse(retrieveAndStore).toRight(NotFound)
      } yield currentWeather

      resultET
        .map(JsonIo.write(_))
        .map(Ok(_))
        .merge
    }
  }

  // suggestions for weather locations, workaround until elastic access, use the storage directly
  // route mapped to /api/weather/suggest
  def suggest(query: String): Action[AnyContent] =
    Action.async {
      timedRequest(s"suggest location for $query") { implicit request =>
        logger.debug(s"suggesting for $query")
        val storage = connectivity.getStorage
        val suggestionsF = storage match {
          case psqlDb: PsqlDbStorage => psqlDb.getWeatherStorage.suggestLocations(query)
          case other =>
            logger.warn(s"$other is not supporting location suggestions")
            Future(Iterable.empty)
        }

        suggestionsF
          .map { suggestions =>
            val normalized = normalize(suggestions)
            logger.debug(s"found ${suggestions.size} suggested locations, normalized to ${normalized.size} ...")
            normalized
          }
          .map(jsonSuggestions => Ok(Json.obj("suggestions" -> jsonSuggestions)))
      }
    }
}
