package dev.cypdashuhn.worldtasker.db

import org.bukkit.NamespacedKey
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

    object TagInheritance : Table() {
        val childId = reference("child_id", Tags)
        val parentId = reference("parent_id", Tags)
        override val primaryKey = PrimaryKey(childId, parentId)
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

    fun findByName(name: String): ResultRow? = transaction {
        Tags.selectAll().where { Tags.name eq name }.firstOrNull()
    }

    fun findByQualifiedName(key: NamespacedKey): ResultRow? {
        val ns = NamespaceManager.findByName(key.namespace) ?: return null
        return findByName(key.key, ns[NamespaceManager.Namespaces.id].value)
    }

    fun findByQualifiedName(qualified: String): ResultRow? {
        val colon = qualified.indexOf(':')
        if (colon == -1) return null
        val nsName = qualified.substring(0, colon)
        val tagName = qualified.substring(colon + 1)
        val ns = NamespaceManager.findByName(nsName) ?: return null
        return findByName(tagName, ns[NamespaceManager.Namespaces.id].value)
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
        val exists = TodoTags.selectAll()
            .where { (TodoTags.todoId eq todoId) and (TodoTags.tagId eq tagId) }
            .firstOrNull() != null
        if (!exists) {
            TodoTags.insert {
                it[TodoTags.todoId] = todoId
                it[TodoTags.tagId] = tagId
            }
        }
    }

    fun removeFromTodo(todoId: Int, tagId: Int) = transaction {
        TodoTags.deleteWhere { (TodoTags.todoId eq todoId) and (TodoTags.tagId eq tagId) }
    }

    fun removeAllForTodo(todoId: Int) = transaction {
        TodoTags.deleteWhere { TodoTags.todoId eq todoId }
    }

    fun tagsForTodo(todoId: Int): List<ResultRow> = transaction {
        (Tags innerJoin TodoTags).selectAll().where { TodoTags.todoId eq todoId }.toList()
    }

    fun tagLabelsForTodo(todoId: Int): List<String> = transaction {
        (Tags innerJoin TodoTags innerJoin NamespaceManager.Namespaces)
            .selectAll()
            .where { TodoTags.todoId eq todoId }
            .map { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
    }

    fun todosForTag(tagId: Int): List<ResultRow> = transaction {
        (TodoManager.Todos innerJoin TodoTags).selectAll().where { TodoTags.tagId eq tagId }.toList()
    }

    fun addInheritance(childId: Int, parentId: Int) = transaction {
        val exists = TagInheritance.selectAll()
            .where { (TagInheritance.childId eq childId) and (TagInheritance.parentId eq parentId) }
            .firstOrNull() != null
        if (!exists) TagInheritance.insert {
            it[TagInheritance.childId] = childId
            it[TagInheritance.parentId] = parentId
        }
    }

    fun removeInheritance(childId: Int, parentId: Int) = transaction {
        TagInheritance.deleteWhere {
            (TagInheritance.childId eq childId) and (TagInheritance.parentId eq parentId)
        }
    }

    fun setInheritance(childId: Int, parentIds: List<Int>) = transaction {
        TagInheritance.deleteWhere { TagInheritance.childId eq childId }
        parentIds.forEach { parentId ->
            TagInheritance.insert {
                it[TagInheritance.childId] = childId
                it[TagInheritance.parentId] = parentId
            }
        }
    }

    fun parentsOf(tagId: Int): List<ResultRow> = transaction {
        val parentIds = TagInheritance.selectAll()
            .where { TagInheritance.childId eq tagId }
            .map { it[TagInheritance.parentId].value }
        if (parentIds.isEmpty()) emptyList()
        else Tags.selectAll().where { Tags.id inList parentIds }.toList()
    }

    /** BFS expansion: returns the given IDs plus all ancestor IDs, cycle-safe. */
    fun expandTagIds(directIds: Set<Int>): Set<Int> = transaction {
        val visited = directIds.toMutableSet()
        val queue = ArrayDeque(directIds.toList())
        while (queue.isNotEmpty()) {
            val tagId = queue.removeFirst()
            TagInheritance.selectAll()
                .where { TagInheritance.childId eq tagId }
                .map { it[TagInheritance.parentId].value }
                .filter { it !in visited }
                .forEach { visited.add(it); queue.add(it) }
        }
        visited
    }

    /** Qualified `namespace:name` labels (direct + all inherited) for a todo — used for DSL query matching. */
    fun expandedTagNamesForTodo(todoId: Int): Set<String> = transaction {
        val directIds = tagsForTodo(todoId).map { it[Tags.id].value }.toSet()
        val allIds = expandTagIds(directIds)
        if (allIds.isEmpty()) emptySet()
        else (Tags innerJoin NamespaceManager.Namespaces)
            .selectAll()
            .where { Tags.id inList allIds.toList() }
            .map { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
            .toSet()
    }

    /** namespace:name labels for tags inherited (but not directly assigned) to a todo. */
    fun inheritedTagLabelsForTodo(todoId: Int): List<String> = transaction {
        val directIds = tagsForTodo(todoId).map { it[Tags.id].value }.toSet()
        val inheritedIds = expandTagIds(directIds) - directIds
        if (inheritedIds.isEmpty()) emptyList()
        else (Tags innerJoin NamespaceManager.Namespaces)
            .selectAll()
            .where { Tags.id inList inheritedIds.toList() }
            .map { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
    }
}
