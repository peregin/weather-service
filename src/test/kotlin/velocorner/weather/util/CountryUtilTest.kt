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
}