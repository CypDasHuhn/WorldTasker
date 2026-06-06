package dev.cypdashuhn.worldtasker.commands.query

import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.cypdashuhn.worldtasker.db.TodoStatus
import org.bukkit.Location
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun executeTodoQuery(query: TodoQuery, playerLocation: Location?): List<ResultRow> {
    // Pre-resolve time filter: all three types query History
    val timeTodoIds: Set<Int>? = query.timeFilter?.let { resolveTimeTodoIds(it) }
    if (timeTodoIds != null && timeTodoIds.isEmpty()) return emptyList()

    // SQL-level filters: name, author, time-IN
    var results: List<ResultRow> = transaction {
        var q = TodoManager.Todos.selectAll()

        query.name?.let { n -> q = q.andWhere { TodoManager.Todos.name like "%$n%" } }
        query.author?.let { a -> q = q.andWhere { TodoManager.Todos.author eq a } }
        timeTodoIds?.let { ids -> q = q.andWhere { TodoManager.Todos.id inList ids.toList() } }

        q.toList()
    }

    // Post-filter: nearRadius
    if (query.nearRadius != null && playerLocation != null) {
        val nearLocIds = TodoManager
            .findNear(playerLocation, query.nearRadius)
            .map { it.id.value }
            .toSet()
        results = results.filter { row -> row[TodoManager.Todos.locationId]?.value in nearLocIds }
    }

    // Post-filter: tags DSL  (+ = AND, , = OR, - = NOT, () = group)
    query.tags?.let { tagsStr ->
        val expr = try {
            parseTagDsl(tagsStr)
        } catch (_: IllegalArgumentException) {
            return@let
        }
        results = results.filter { row ->
            val todoId = row[TodoManager.Todos.id].value
            val allNames = TagManager.expandedTagNamesForTodo(todoId)
            expr.matches(allNames)
        }
    }

    // Post-filter: state — always hide DELETED; hide COMPLETED unless --completed is set
    results = results.filter { row ->
        when (TodoManager.stateOf(row[TodoManager.Todos.id].value)) {
            TodoState.DELETED -> false
            TodoState.COMPLETED -> query.showCompleted
            TodoState.ACTIVE -> true
        }
    }

    return results
}

private fun resolveTimeTodoIds(tf: TimeFilter): Set<Int> {
    val start = tf.date.atStartOfDay()
    val end = tf.date.plusDays(1).atStartOfDay()
    val statusFilter = when (tf.type) {
        TimeType.CREATED -> TodoStatus.CREATE
        TimeType.COMPLETED -> TodoStatus.COMPLETE
        TimeType.WORKED -> TodoStatus.WORK
    }
    return transaction {
        var q = HistoryManager.History
            .selectAll()
            .andWhere { HistoryManager.History.status eq statusFilter }
        q = when (tf.operator) {
            TimeOperator.BEFORE -> q.andWhere { HistoryManager.History.time less start }

            TimeOperator.AFTER -> q.andWhere { HistoryManager.History.time greaterEq end }

            TimeOperator.ON -> q.andWhere {
                (HistoryManager.History.time greaterEq start) and
                    (HistoryManager.History.time less end)
            }
        }
        q.map { it[HistoryManager.History.todoId].value }.toSet()
    }
}
