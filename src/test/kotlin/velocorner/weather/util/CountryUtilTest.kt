package velocorner.weather.util

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
}