package velocorner.weather.repo

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.update
import velocorner.weather.model.GeoPosition
import velocorner.weather.repo.DatabaseFactory.transact
import velocorner.weather.repo.LocationTable.latitude
import velocorner.weather.repo.LocationTable.longitude

interface LocationRepo {
    suspend fun store(location: String, position: GeoPosition)
    suspend fun getPosition(location: String): GeoPosition?
    suspend fun suggestLocations(snippet: String): List<String>
}

object LocationTable : Table("location") {
    val location = text("location")
    val latitude = double("latitude")
    val longitude = double("longitude")
    override val primaryKey = PrimaryKey(location)
}

class LocationRepoImpl : LocationRepo {

    override suspend fun store(location: String, position: GeoPosition) {
        transact {
            val count = LocationTable.insertIgnore {
                it[LocationTable.location] = location.lowercase()
                it[latitude] = position.latitude
                it[longitude] = position.longitude
            }.insertedCount
            if (count == 0) {
                LocationTable.update({ LocationTable.location eq location.lowercase() }) {
                    it[latitude] = position.latitude
                    it[longitude] = position.longitude
                }
            }
        }
    }

    override suspend fun getPosition(location: String): GeoPosition? = transact {
        LocationTable.select(listOf(latitude, longitude)).where { LocationTable.location eq location.lowercase() }
            .map { GeoPosition(it[latitude], it[longitude]) }.singleOrNull()
    }

    override suspend fun suggestLocations(snippet: String): List<String> = transact {
        LocationTable.select(LocationTable.location).where {
            LocationTable.location.like("%${snippet.lowercase()}%")
        }.map { it[LocationTable.location] }.distinct()
    }
}