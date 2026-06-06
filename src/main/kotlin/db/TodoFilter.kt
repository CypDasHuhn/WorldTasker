package dev.cypdashuhn.worldtasker.db

import dev.rooster.db.utility_tables.LocationManager
import org.jetbrains.exposed.sql.transactions.transaction

enum class TagFilterState { NEUTRAL, INCLUDE, EXCLUDE }
enum class StatusFilter { DEFAULT, ALL, COMPLETED }

data class TodoFilter(
    val included: List<Int> = emptyList(),
    val excluded: List<Int> = emptyList(),
    val statusFilter: StatusFilter = StatusFilter.DEFAULT,
    val authorIncluded: List<String> = emptyList(),
    val authorExcluded: List<String> = emptyList(),
    val distanceRadius: Int = 0,
    val distanceCenterX: Double = 0.0,
    val distanceCenterZ: Double = 0.0,
    val distanceCenterWorld: String = "",
) {
    val distanceEnabled get() = distanceRadius > 0 && distanceCenterWorld.isNotEmpty()

    fun toggle(tagId: Int): TodoFilter = when {
        tagId !in included && tagId !in excluded -> copy(included = included + tagId)
        tagId in included -> copy(included = included - tagId, excluded = excluded + tagId)
        else -> copy(excluded = excluded - tagId)
    }

    fun stateOf(tagId: Int): TagFilterState = when {
        tagId in included -> TagFilterState.INCLUDE
        tagId in excluded -> TagFilterState.EXCLUDE
        else -> TagFilterState.NEUTRAL
    }

    fun toggleAuthor(name: String): TodoFilter = when {
        name !in authorIncluded && name !in authorExcluded -> copy(authorIncluded = authorIncluded + name)
        name in authorIncluded -> copy(authorIncluded = authorIncluded - name, authorExcluded = authorExcluded + name)
        else -> copy(authorExcluded = authorExcluded - name)
    }

    fun authorStateOf(name: String): TagFilterState = when {
        name in authorIncluded -> TagFilterState.INCLUDE
        name in authorExcluded -> TagFilterState.EXCLUDE
        else -> TagFilterState.NEUTRAL
    }

    fun isEmpty() = included.isEmpty() && excluded.isEmpty()
        && statusFilter == StatusFilter.DEFAULT
        && authorIncluded.isEmpty() && authorExcluded.isEmpty()
        && !distanceEnabled

    /** Only checks tag and distance matching — status/author are evaluated in TodoManager.filteredIds. */
    fun matches(todoId: Int, locationId: Int? = null): Boolean {
        if (included.isNotEmpty() || excluded.isNotEmpty()) {
            val directIds = TagManager.tagsForTodo(todoId).map { it[TagManager.Tags.id].value }.toSet()
            val allTagIds = TagManager.expandTagIds(directIds)
            if (excluded.any { it in allTagIds }) return false
            if (included.isNotEmpty() && included.none { it in allTagIds }) return false
        }

        if (distanceEnabled) {
            if (locationId == null) return false
            val loc = transaction { LocationManager.Location.findById(locationId) } ?: return false
            if (loc.worldName != distanceCenterWorld) return false
            val dx = loc.x - distanceCenterX
            val dz = loc.z - distanceCenterZ
            if (dx * dx + dz * dz > distanceRadius.toDouble() * distanceRadius) return false
        }

        return true
    }
}
