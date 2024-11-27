package velocorner.weather.util

import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.assertAll
import velocorner.weather.util.CountryUtil.beautify
import velocorner.weather.util.CountryUtil.normalize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class CountryUtilTest {

    @Test
    fun `read the country codes from json`() {
        val name2Code = CountryUtil.readCountries()
        assert(name2Code.isNotEmpty())
        assertEquals(requireNotNull(name2Code["switzerland"]), "CH")
        assertEquals(requireNotNull(name2Code["hungary"]), "HU")
        assertNull(name2Code["unknown"])
    }

    @Test
    fun `read the capitals from json`() {
        val code2Capital = CountryUtil.readCapitals()
        assertEquals(requireNotNull(code2Capital["CH"]), "Berne")
        assertEquals(requireNotNull(code2Capital["HU"]), "Budapest")
        assertNull(code2Capital["unknown"])
    }

    @Test
    fun `should handle basic country conversions`() {
        assertEquals("Zurich", CountryUtil.iso("Zurich"))
        assertEquals("Zurich,CH", CountryUtil.iso("Zurich,Switzerland"))
        assertEquals("Budapest,HU", CountryUtil.iso("Budapest,Hungary"))
        assertEquals("Paris,FR", CountryUtil.iso("Paris,France"))
    }

    @Test
    fun `should handle spaces in location names`() {
        assertEquals("Zurich,CH", CountryUtil.iso("Zurich, Switzerland"))
        assertEquals("Zurich,CH", CountryUtil.iso(" Zurich , Switzerland "))
        assertEquals("finale ligure,IT", CountryUtil.iso("finale ligure, Italy"))
    }

    @Test
    fun `should handle case insensitive country names`() {
        assertEquals("budapest,HU", CountryUtil.iso("budapest, hungary"))
        assertEquals("Zurich,CH", CountryUtil.iso("Zurich,SWITZERLAND"))
        assertEquals("Zurich,CH", CountryUtil.iso("Zurich,switzerland"))
    }

    @Test
    fun `should handle unknown countries`() {
        assertEquals("Zurich, Helvetica", CountryUtil.iso("Zurich, Helvetica"))
        assertEquals("City,Unknown", CountryUtil.iso("City,Unknown"))
    }

    @Test
    fun `should handle single word locations`() {
        assertEquals("London", CountryUtil.iso("London"))
    }

    @Test
    fun `should handle empty and invalid inputs`() {
        assertEquals("", CountryUtil.iso(""))
        assertEquals(",", CountryUtil.iso(","))
        assertEquals("City,", CountryUtil.iso("City,"))
        assertEquals(",Country", CountryUtil.iso(",Country"))
    }

    @Test
    fun `should beautify locations correctly`() {
        assertAll(
            { assertEquals("Adliswil, CH", beautify("adliswil, ch")) },
            { assertEquals("Abu Dhabi, AE", beautify("abu dhabi, ae")) },
            { assertEquals("Buenos Aires, AR", beautify("buenos aires, ar")) },
            { assertEquals("Budapest", beautify("budapest")) },
            { assertEquals("New York City, US", beautify("new york city, us")) }
        )
    }

    @Test
    fun `should handle edge cases`() {
        assertAll(
            { assertEquals("City", beautify("  city  ")) },
            { assertEquals("City, XX", beautify("city,xx")) },
            { assertEquals("City, Country", beautify("city, country")) },
            { assertEquals("City", beautify("city,")) },
            { assertEquals("Budapest", beautify("budapest")) },
            { assertEquals("Multi Word City", beautify("multi word city")) }
        )
    }

    @Test
    fun `beautify should handle special characters`() {
        assertAll(
            { assertEquals("São Paulo, BR", beautify("são paulo, br")) },
            { assertEquals("Zürich, CH", beautify("zürich, ch")) }
        )
    }

    @Test
    fun `should normalize locations correctly`() {
        val input = listOf(
            "adliswil, ch",
            "Adliswil, ch",
            "adliswil",
            "adliswil,CH",
            "Adliswil,CH",
            "Adliswil"
        )

        val result = normalize(input)

        assertEquals(1, result.size)
        assertEquals("Adliswil,CH", result.first())
    }

    @Test
    fun `should handle empty input`() {
        assertEquals(emptyList(), normalize(emptyList()))
    }

    @Test
    fun `should handle multiple cities`() {
        val input = listOf(
            "Zurich, CH",
            "zurich, ch",
            "bern, ch",
            "Bern, CH",
            "geneva, ch"
        )

        val result = normalize(input)

        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf(
            "Zurich, CH",
            "Bern, CH",
            "geneva, ch"
        )))
    }

    @Test
    fun `should handle multi-word cities`() {
        val input = listOf(
            "new york, us",
            "New York, US",
            "new york,US"
        )

        val result = normalize(input)

        assertEquals(1, result.size)
        assertEquals("New York, US", result.first())
    }

    @Test
    fun `normalize should handle special characters`() {
        val input = listOf(
            "são paulo, br",
            "São Paulo, BR",
            "são paulo, br"
        )

        val result = normalize(input)

        assertEquals(1, result.size)
        assertEquals("São Paulo, BR", result.first())
    }
}