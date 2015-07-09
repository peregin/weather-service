package velocorner.model

import org.joda.time.LocalDate
import org.specs2.mutable.Specification

/**
 * Created by levi on 09/07/15.
 */
class YearlyProgressSpec extends Specification {

  val today = LocalDate.parse("2015-07-10")
  val progress = Progress(1, 10, 10, 1000, 3, 30, 30)

  "model" should {
    "aggregate previous items" in {
      val yp = YearlyProgress(2015, List(
        DailyProgress(today, progress),
        DailyProgress(today.plusDays(1), progress),
        DailyProgress(today.plusDays(2), progress)
      ))
      val ap = YearlyProgress.aggregate(List(yp))
      ap must haveSize(1)
      val adp = ap(0).progress
      adp must haveSize(3)
      adp(0).progress.rides === 1
      adp(1).progress.rides === 2
      adp(2).progress.rides === 3
    }
  }
}
