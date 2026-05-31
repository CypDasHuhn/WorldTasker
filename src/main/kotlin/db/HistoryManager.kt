package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

enum class TodoStatus { CREATE, WORK, COMPLETE, DELETE }

object HistoryManager {
    object History : IntIdTable() {
        val todoId = reference("todo_id", TodoManager.Todos)
        val time = datetime("time")
        val status = enumerationByName<TodoStatus>("status", 16)
        val comment = text("comment").nullable()
    }

    fun record(todoId: Int, status: TodoStatus, comment: String? = null) = transaction {
        History.insert {
            it[History.todoId] = todoId
            it[History.time] = LocalDateTime.now()
            it[History.status] = status
            it[History.comment] = comment
        }
    }

    fun forTodo(todoId: Int): List<ResultRow> = transaction {
        History.selectAll()
            .where { History.todoId eq todoId }
            .orderBy(History.time, SortOrder.ASC)
            .toList()
    }

    fun latestForTodo(todoId: Int): ResultRow? = transaction {
        History.selectAll()
            .where { History.todoId eq todoId }
            .orderBy(History.time, SortOrder.DESC)
            .firstOrNull()
    }
}
