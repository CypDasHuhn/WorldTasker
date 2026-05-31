package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

enum class TodoStatus { CREATE, WORK, COMPLETE, REACTIVATE, DELETE }

object HistoryManager {
    object History : IntIdTable() {
        val todoId = reference("todo_id", TodoManager.Todos)
        val time = datetime("time")
        val author = text("author")
        val status = enumerationByName<TodoStatus>("status", 16)
        val comment = text("comment").nullable()
    }

    fun record(todoId: Int, author: String, status: TodoStatus, comment: String? = null) = transaction {
        History.insert {
            it[History.todoId] = todoId
            it[History.time] = LocalDateTime.now()
            it[History.author] = author
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

    fun createdAtForTodo(todoId: Int): java.time.LocalDateTime? = transaction {
        History.selectAll()
            .where { (History.todoId eq todoId) and (History.status eq TodoStatus.CREATE) }
            .orderBy(History.time, SortOrder.ASC)
            .firstOrNull()
            ?.get(History.time)
    }
}
