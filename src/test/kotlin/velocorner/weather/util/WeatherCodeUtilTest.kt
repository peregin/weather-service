package velocorner.weather.util

import kotlin.test.*
import kotlin.test.Test

internal class WeatherCodeUtilTest {
    @Test fun weatherCodeMappings() {
        val map = WeatherCodeUtil.fromResources()
        assertEquals(54, map.size)
        assertEquals(WeatherCode(200, "thunderstorm with light rain", "icon-weather-005", "WiThunderstorm"), map[200])
        assertEquals(WeatherCode(804, "overcast clouds", "icon-weather-022", "WiCloudy"), map[804])
    }

    @Test fun bootstrapIconMappings() {
        assertEquals("icon-weather-005", WeatherCodeUtil.bootstrapIcon(200))
        assertEquals("icon-weather-031", WeatherCodeUtil.bootstrapIcon(511))
    }
}