package dev.cypdashuhn.worldtasker.db

import dev.cypdashuhn.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.worldtasker.WorldTaskerPlugin
import org.bukkit.Location
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object TodoManager {
    object Todos : IntIdTable() {
        val name = text("name")
        val author = text("author")
        val description = text("description")
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
        val locationId = reference("location_id", LocationManager.Locations).nullable()
    }

    fun create(
        name: String,
        author: String,
        description: String,
        location: Location?,
    ): Int {
        val now = LocalDateTime.now()
        return transaction {
            val locId =
                location?.let {
                    WorldTaskerPlugin.locationManager.insertOrGetLocation(it)
                    LocationManager.Location
                        .find {
                            (LocationManager.Locations.x eq it.x) and
                                (LocationManager.Locations.y eq it.y) and
                                (LocationManager.Locations.z eq it.z) and
                                (LocationManager.Locations.worldName eq it.world.name)
                        }.first()
                        .id
                }
            Todos.insert {
                it[name] = name
                it[author] = author
                it[description] = description
                it[createdAt] = now
                it[updatedAt] = now
                it[locationId] = locId
            }[Todos.id].value
        }
    }

    fun findNear(
        playerLocation: Location,
        chunkRadius: Int,
    ): List<LocationManager.Location> {
        val blockRadius = chunkRadius * 16.0
        return transaction {
            LocationManager.Location
                .find { LocationManager.Locations.worldName eq playerLocation.world.name }
                .filter {
                    val dx = it.x - playerLocation.x
                    val dz = it.z - playerLocation.z
                    dx * dx + dz * dz <= blockRadius * blockRadius
                }
        }
    }
}
