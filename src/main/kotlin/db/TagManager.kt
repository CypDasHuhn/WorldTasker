package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object TagManager {
    object Tags : IntIdTable() {
        val name = varchar("name", 64)
        val namespaceId = reference("namespace_id", NamespaceManager.Namespaces)
    }

    object TodoTags : Table() {
        val todoId = reference("todo_id", TodoManager.Todos)
        val tagId = reference("tag_id", Tags)
        override val primaryKey = PrimaryKey(todoId, tagId)
    }

    fun create(name: String, namespaceId: Int): Int = transaction {
        Tags.insert {
            it[Tags.name] = name
            it[Tags.namespaceId] = namespaceId
        }[Tags.id].value
    }

    fun rename(id: Int, name: String) = transaction {
        Tags.update({ Tags.id eq id }) { it[Tags.name] = name }
    }

    fun find(id: Int): ResultRow? = transaction {
        Tags.selectAll().where { Tags.id eq id }.firstOrNull()
    }

    fun findByName(name: String, namespaceId: Int): ResultRow? = transaction {
        Tags.selectAll().where { (Tags.name eq name) and (Tags.namespaceId eq namespaceId) }.firstOrNull()
    }

    fun all(): List<ResultRow> = transaction {
        Tags.selectAll().toList()
    }

    fun byNamespace(namespaceId: Int): List<ResultRow> = transaction {
        Tags.selectAll().where { Tags.namespaceId eq namespaceId }.toList()
    }

    fun delete(id: Int) = transaction {
        Tags.deleteWhere { Tags.id eq id }
    }

    fun addToTodo(todoId: Int, tagId: Int) = transaction {
        TodoTags.insert {
            it[TodoTags.todoId] = todoId
            it[TodoTags.tagId] = tagId
        }
    }

    fun removeFromTodo(todoId: Int, tagId: Int) = transaction {
        TodoTags.deleteWhere { (TodoTags.todoId eq todoId) and (TodoTags.tagId eq tagId) }
    }

    fun tagsForTodo(todoId: Int): List<ResultRow> = transaction {
        (Tags innerJoin TodoTags).selectAll().where { TodoTags.todoId eq todoId }.toList()
    }

    fun todosForTag(tagId: Int): List<ResultRow> = transaction {
        (TodoManager.Todos innerJoin TodoTags).selectAll().where { TodoTags.tagId eq tagId }.toList()
    }
}
