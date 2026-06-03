package dev.cypdashuhn.worldtasker.db

enum class TagFilterState { NEUTRAL, INCLUDE, EXCLUDE }

data class TodoFilter(
    val included: List<Int> = emptyList(),
    val excluded: List<Int> = emptyList()
) {
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

    fun isEmpty() = included.isEmpty() && excluded.isEmpty()

    fun matches(todoId: Int): Boolean {
        if (isEmpty()) return true
        val directIds = TagManager.tagsForTodo(todoId).map { it[TagManager.Tags.id].value }.toSet()
        val allTagIds = TagManager.expandTagIds(directIds)
        if (excluded.any { it in allTagIds }) return false
        if (included.isNotEmpty() && included.none { it in allTagIds }) return false
        return true
    }
}
