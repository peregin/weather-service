package velocorner.search.manual.brand

import com.sksamuel.elastic4s.ElasticDsl._
import com.typesafe.scalalogging.LazyLogging
import velocorner.manual.AwaitSupport
import velocorner.search.MarketplaceElasticSupport

object SearchBrandElasticManual extends App with MarketplaceElasticSupport with AwaitSupport with LazyLogging {

  logger.info("initialized...")

  val searchTerm = "Schwalbe"
  val hits = searchBrands(searchTerm).await
  logger.info(s"results[$searchTerm]: ${hits.mkString("\n", "\n", "\n")}")

  val suggestionText = "sch"
  val suggestions = suggestBrands(suggestionText).await
  logger.info(s"suggestions[$suggestionText]: $suggestions")

  elastic.close()
}
