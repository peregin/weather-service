package velocorner.weather.util

import org.slf4j.LoggerFactory

/**
 * Utility to convert the weather code mappings into the model.
 *
 * # Group 5xx: Rain
 * # ID	Meaning	                    Icon BootstrapIcon    ReactIcon
 * 500	light rain	                10d icon-weather-008    WiRainMix
 * 501	moderate rain	            10d icon-weather-007    WiRain
 */

data class WeatherCode(val code: Int, val meaning: String, val bootstrapIcon: String, val reactIcon: String)

class WeatherCodeUtil {

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)
        private val code2Model by lazy {
            fromResources()
        }

        fun fromResources(): Map<Int, WeatherCode> {
            val url = WeatherCodeUtil::class.java.getResource("/weather_codes.txt")
            val entries = url!!.readText().lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filter { !it.startsWith("#") }
                .map { parse(it) }

            // log errors if any
            entries.filter { it.isFailure }
                .forEach { logger.error("failed to parse line ${it.exceptionOrNull()?.message}") }

            return entries.filter { it.isSuccess }.map { it.getOrThrow() }.associate { e -> (e.code to e) }
        }

        fun parse(line: String): Result<WeatherCode> = runCatching {
            val tokens = line.split("\t").map { it.trim() }
            val iconParts = tokens[2].split(" ")
            WeatherCode(
                code = tokens[0].toInt(),
                meaning = tokens[1],
                bootstrapIcon = iconParts[1],
                reactIcon = iconParts.last()
            )
        }

        fun bootstrapIcon(code: Int): String = code2Model[code]?.bootstrapIcon ?: error("invalid weather code $code")
        fun reactIcon(code: Int): String = code2Model[code]?.reactIcon ?: error("invalid weather code $code")
    }
}