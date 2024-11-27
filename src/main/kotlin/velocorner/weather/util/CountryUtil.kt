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

    /**
     * Converts location string to use ISO country code
     * @param location Location string in format "city,country"
     * @return Location string with ISO country code or original string if conversion not possible
     * @example "Zurich,Switzerland" -> "Zurich,CH"
     */
    fun iso(location: String): String {
        val index = location.indexOf(',')
        if (index > -1) {
            val city = location.substring(0, index).trim()
            val country = location.substring(index + 1).trim().lowercase()
            val isoCode = country2Code[country]
            return if (isoCode != null) {
                "$city,$isoCode"
            } else {
                location.trim()
            }
        }
        return location.trim()
    }

    // used to read the locations table, where everything is lowercase
    // converts to a format that is presented in the weather location widget:
    // adliswil, ch -> Adliswil, CH
    // abu dhabi, ae -> Abu Dhabi, AE
    // buenos aires, ar -> Buenos Aires, AR
    // budapest -> Budapest
    fun beautify(location: String): String {
        val locationRaw = location.trim()
        val index = locationRaw.lastIndexOf(',')
        val (city, countryCode) = when (index) {
            -1 -> Pair(locationRaw, null)
            // remove the last character
            locationRaw.length - 1 -> Pair(locationRaw.substring(0, index).trim(), null)
            else -> Pair(
                locationRaw.substring(0, index).trim(),
                locationRaw.substring(index + 1).trim()
            )
        }

        val beautifiedCity = city.split(" ")
            .map { it.trim().replaceFirstChar(Char::uppercase) }
            .joinToString(" ")

        return when {
            countryCode == null -> beautifiedCity
            countryCode.length == 2 -> "$beautifiedCity, ${countryCode.uppercase()}"
            else -> "$beautifiedCity, ${countryCode.replaceFirstChar(Char::uppercase)}"
        }
    }

    // some suggestions are similar, see:
    // "adliswil, ch"
    // "Adliswil, ch"
    // "adliswil"
    // "adliswil,CH"
    // "Adliswil,CH"
    // "Adliswil"
    // ----------------
    // normalized it to Adliswil,CH
    fun normalize(locations: List<String>): List<String> {
        val place2Locations = locations
            .map { location ->
                val index = location.lastIndexOf(',')
                when (index) {
                    -1 -> location.trim() to location
                    else -> location.substring(0, index).trim() to location
                }
            }
            .map { (place, location) -> place.lowercase() to location }
            .groupBy({ it.first }, { it.second })

        return place2Locations.mapValues { (_, list) ->
            list.fold("") { result, item ->
                when {
                    result.isEmpty() && item.isEmpty() -> ""
                    result.isEmpty() -> item
                    item.isEmpty() -> result
                    // Prefer capitalized first letter
                    result.first().isUpperCase() && item.first().isLowerCase() -> result
                    result.first().isLowerCase() && item.first().isUpperCase() -> item
                    // Prefer entries with country code
                    result.contains(',') && !item.contains(',') -> result
                    !result.contains(',') && item.contains(',') -> item
                    // Prefer uppercase country codes
                    result.last().isUpperCase() && item.last().isLowerCase() -> result
                    result.last().isLowerCase() && item.last().isUpperCase() -> item
                    // Default to first match
                    else -> result
                }
            }
        }.values.toList()
    }
}

