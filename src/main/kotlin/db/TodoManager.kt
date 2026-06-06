package dev.cypdashuhn.worldtasker.db

import dev.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.worldtasker.WorldTaskerPlugin
import org.bukkit.Location
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import dev.cypdashuhn.worldtasker.db.StatusFilter
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

enum class TodoState { ACTIVE, COMPLETED, DELETED }

object TodoManager {
    object Todos : IntIdTable() {
        val name = text("name")
        val author = text("author")
        val description = text("description")
        val locationId = reference("location_id", LocationManager.Locations).nullable()
    }

    class TodoEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<TodoEntry>(Todos)

        val name by Todos.name
        val author by Todos.author
        val description by Todos.description
        val locationId by Todos.locationId
    }

    fun create(
        name: String,
        player: Player,
        description: String,
        location: Location?,
    ): Int = transaction {
        val locId = location?.let {
            WorldTaskerPlugin.locationManager.insertOrGetLocation(it)
            LocationManager.Location
                .find {
                    (LocationManager.Locations.x eq it.x) and
                            (LocationManager.Locations.y eq it.y) and
                            (LocationManager.Locations.z eq it.z) and
                            (LocationManager.Locations.worldName eq it.world.name)
                }.first().id
        }
        val id = Todos.insert {
            it[Todos.name] = name
            it[Todos.author] = player.name
            it[Todos.description] = description
            it[locationId] = locId
        }[Todos.id].value
        HistoryManager.record(id, player, TodoStatus.CREATE)
        id
    }

    fun findByName(name: String): ResultRow? = transaction {
        Todos.selectAll().where { Todos.name eq name }.firstOrNull()
    }

    fun findById(id: Int): TodoEntry? = transaction {
        Todos.selectAll().where { Todos.id eq id }.firstOrNull().let{ if (it == null) null else TodoEntry.wrapRow(it) }
    }

    fun updateName(id: Int, name: String) = transaction {
        Todos.update({ Todos.id eq id }) { it[Todos.name] = name }
    }

    fun updateDescription(id: Int, description: String) = transaction {
        Todos.update({ Todos.id eq id }) { it[Todos.description] = description }
    }

    fun complete(id: Int, player: Player) =
        HistoryManager.record(id, player, TodoStatus.COMPLETE)

    fun reactivate(id: Int, player: Player) =
        HistoryManager.record(id, player, TodoStatus.REACTIVATE)

    fun delete(id: Int, player: Player) =
        HistoryManager.record(id, player, TodoStatus.DELETE)

    fun stateOf(todoId: Int): TodoState = transaction {
        val latest = HistoryManager.History.selectAll()
            .where {
                (HistoryManager.History.todoId eq todoId) and
                        (HistoryManager.History.status inList listOf(
                            TodoStatus.CREATE, TodoStatus.COMPLETE,
                            TodoStatus.REACTIVATE, TodoStatus.DELETE
                        ))
            }
            .orderBy(HistoryManager.History.time, SortOrder.DESC)
            .firstOrNull()
        when (latest?.get(HistoryManager.History.status)) {
            TodoStatus.COMPLETE -> TodoState.COMPLETED
            TodoStatus.DELETE -> TodoState.DELETED
            else -> TodoState.ACTIVE
        }
    }

    fun filteredIds(filter: TodoFilter): List<Int> = transaction {
        Todos.selectAll().toList()
            .filter { row ->
                val todoId = row[Todos.id].value
                val state = stateOf(todoId)

                val statusOk = when (filter.statusFilter) {
                    StatusFilter.DEFAULT -> state == TodoState.ACTIVE
                    StatusFilter.ALL -> state != TodoState.DELETED
                    StatusFilter.COMPLETED -> state == TodoState.COMPLETED
                }
                if (!statusOk) return@filter false

                val author = row[Todos.author]
                if (filter.authorIncluded.isNotEmpty() && author !in filter.authorIncluded) return@filter false
                if (author in filter.authorExcluded) return@filter false

                val locId = row[Todos.locationId]?.value
                filter.matches(todoId, locId)
            }
            .map { it[Todos.id].value }
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
