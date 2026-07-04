package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object SuggestionCache {
    data class CachedTodo(
        val id: Int,
        val name: String,
        val state: TodoState,
        val author: String
    )

    @Volatile private var todos: List<CachedTodo>? = null

    @Volatile private var namespaceNames: List<String>? = null

    @Volatile private var tagNames: List<String>? = null

    @Volatile private var authors: List<String>? = null

    fun getTodos(): List<CachedTodo> = todos ?: loadTodos().also { todos = it }

    fun getNamespaceNames(): List<String> = namespaceNames ?: loadNamespaceNames().also { namespaceNames = it }

    fun getTagNames(): List<String> = tagNames ?: loadTagNames().also { tagNames = it }

    fun getAuthors(): List<String> = authors ?: loadAuthors().also { authors = it }

    fun invalidateAll() {
        todos = null
        namespaceNames = null
        tagNames = null
        authors = null
    }

    private fun loadTodos(): List<CachedTodo> =
        transaction {
            val rows = TodoManager.Todos.selectAll().toList()
            val todoIds = rows.map { it[TodoManager.Todos.id].value }

            val stateMap = if (todoIds.isNotEmpty()) {
                HistoryManager.History
                    .selectAll()
                    .where {
                        (HistoryManager.History.todoId inList todoIds) and
                            (HistoryManager.History.status inList listOf(
                                TodoStatus.CREATE, TodoStatus.COMPLETE,
                                TodoStatus.REACTIVATE, TodoStatus.DELETE,
                            ))
                    }.orderBy(HistoryManager.History.time, SortOrder.DESC)
                    .toList()
                    .groupBy { it[HistoryManager.History.todoId].value }
                    .mapValues { (_, events) ->
                        when (events.first()[HistoryManager.History.status]) {
                            TodoStatus.COMPLETE -> TodoState.COMPLETED
                            TodoStatus.DELETE -> TodoState.DELETED
                            else -> TodoState.ACTIVE
                        }
                    }
            } else {
                emptyMap()
            }

            rows.map { row ->
                val id = row[TodoManager.Todos.id].value
                CachedTodo(
                    id = id,
                    name = row[TodoManager.Todos.name],
                    state = stateMap[id] ?: TodoState.ACTIVE,
                    author = row[TodoManager.Todos.author]
                )
            }
        }

    private fun loadNamespaceNames(): List<String> =
        transaction {
            NamespaceManager.all().map { it[NamespaceManager.Namespaces.name] }
        }

    private fun loadTagNames(): List<String> =
        transaction {
            (TagManager.Tags innerJoin NamespaceManager.Namespaces)
                .selectAll()
                .map { "${it[NamespaceManager.Namespaces.name]}:${it[TagManager.Tags.name]}" }
        }

    private fun loadAuthors(): List<String> =
        transaction {
            TodoManager.Todos
                .selectAll()
                .map { it[TodoManager.Todos.author] }
                .distinct()
        }
}
