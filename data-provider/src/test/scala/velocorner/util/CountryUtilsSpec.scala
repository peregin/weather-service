package velocorner.util

import org.specs2.mutable.Specification

class CountryUtilsSpec extends Specification {

  "weather location" should {

    "read the json list" in {
      val name2Code = CountryUtils.readCountries()
      name2Code must not be empty
      name2Code.get("switzerland") === Some("CH")
      name2Code.get("hungary") === Some("HU")
    }

    "be converted to ISO country code" in {
      CountryUtils.iso("Zurich") === "Zurich"
      CountryUtils.iso("Zurich, Switzerland") === "Zurich,CH"
      CountryUtils.iso("Zurich, Helvetica") === "Zurich, Helvetica"
      CountryUtils.iso("Budapest, Hungary") === "Budapest,HU"
    }

    "normalize list of locations" in {
      val locations = List(
        "adliswil, ch",
        "Adliswil, ch",
        "adliswil",
        "adliswil,CH",
        "Adliswil,CH",
        "Adliswil",
        "Budapest"
      )
      CountryUtils.normalize(locations) should containTheSameElementsAs(List("Adliswil,CH", "Budapest"))
    }
  }

  "country code2" should {

    "be converted to capital" in {
      CountryUtils.code2Capital("CH") === "Berne"
      CountryUtils.code2Capital("HU") === "Budapest"
    }
  }
}
