package velocorner.weather.util

import velocorner.weather.model.CountryIso
import velocorner.weather.util.ResourceUtil.load

/**
 * Utility:
 * 1./ to convert a country name (if given) into 2-letter ISO standard.
 * <city[,country]>
 * E.g.
 * Zurich,Switzerland = Zurich,CH
 * London = London
 * 2./ determine capital of a country
 * CH -> Bern
 */
object CountryUtil {

    // Switzerland -> CH
    val country2Code: Map<String, String> by lazy { readCountries() } // lowercase name -> ISO code2

    // CH -> Bern
    val code2Capital: Map<String, String> by lazy { readCapitals() }

    fun readCountries(): Map<String, String> =
        load<List<CountryIso>>("/countries.json")
            .associate { country ->
                country.name.lowercase() to country.code
            }


    fun readCapitals(): Map<String, String> =
        load<Map<String, String>>("/capitals.json")
}

