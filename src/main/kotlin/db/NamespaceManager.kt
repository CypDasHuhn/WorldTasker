package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

enum class NamespaceDeleteResult { DELETED, BLOCKED_SCOPE, BLOCKED_HAS_TAGS }

sealed class NamespaceModeChangeResult {
    object Changed : NamespaceModeChangeResult()
    data class MultipleTagsViolation(val todoCount: Int) : NamespaceModeChangeResult()
}

sealed class NamespaceCreateResult {
    data class Created(
        val id: Int
    ) : NamespaceCreateResult()

    object ReservedName : NamespaceCreateResult()

    object DuplicateName : NamespaceCreateResult()
}

enum class NamespaceRenameResult { RENAMED, RESERVED_NAME, DUPLICATE_NAME }

object NamespaceManager {
    object Namespaces : IntIdTable() {
        val name = varchar("name", 64).uniqueIndex()
        val material = varchar("material", 64).default("BOOKSHELF")
        val allowsMultiple = bool("allows_multiple").default(true)
    }

    fun create(name: String, allowsMultiple: Boolean = true): NamespaceCreateResult {
        if (name in RESERVED_NAMES) return NamespaceCreateResult.ReservedName
        if (findByName(name) != null) return NamespaceCreateResult.DuplicateName
        val id = transaction {
            Namespaces
                .insert {
                    it[Namespaces.name] = name
                    it[Namespaces.allowsMultiple] = allowsMultiple
                }[Namespaces.id]
                .value
        }
        return NamespaceCreateResult.Created(id)
    }

    fun rename(id: Int, name: String): NamespaceRenameResult {
        if (name in RESERVED_NAMES) return NamespaceRenameResult.RESERVED_NAME
        val existing = findByName(name)
        if (existing != null && existing[Namespaces.id].value != id) return NamespaceRenameResult.DUPLICATE_NAME
        transaction { Namespaces.update({ Namespaces.id eq id }) { it[Namespaces.name] = name } }
        TodoScopeManager.onNamespaceRenamed(id, name)
        return NamespaceRenameResult.RENAMED
    }

    fun updateMaterial(id: Int, material: String) =
        transaction {
            Namespaces.update({ Namespaces.id eq id }) { it[Namespaces.material] = material }
        }

    fun setAllowsMultiple(id: Int, allowsMultiple: Boolean): NamespaceModeChangeResult {
        if (!allowsMultiple) {
            val violatingCount = countTodosWithMultipleTagsFromNamespace(id)
            if (violatingCount > 0) return NamespaceModeChangeResult.MultipleTagsViolation(violatingCount)
        }
        transaction {
            Namespaces.update({ Namespaces.id eq id }) { it[Namespaces.allowsMultiple] = allowsMultiple }
        }
        return NamespaceModeChangeResult.Changed
    }

    fun countTodosWithMultipleTagsFromNamespace(namespaceId: Int): Int =
        transaction {
            val tagIds = TagManager.Tags
                .selectAll()
                .where { TagManager.Tags.namespaceId eq namespaceId }
                .map { it[TagManager.Tags.id].value }
            if (tagIds.isEmpty()) return@transaction 0
            TagManager.TodoTags
                .selectAll()
                .where { TagManager.TodoTags.tagId inList tagIds }
                .groupBy { it[TagManager.TodoTags.todoId].value }
                .count { it.value.size > 1 }
        }

    fun isSingleTagNamespace(id: Int): Boolean =
        transaction {
            Namespaces
                .selectAll()
                .where { Namespaces.id eq id }
                .firstOrNull()
                ?.get(Namespaces.allowsMultiple)
                ?.not() ?: false
        }

    fun singleTagNamespaceIds(): Set<Int> =
        transaction {
            Namespaces
                .selectAll()
                .where { Namespaces.allowsMultiple eq false }
                .map { it[Namespaces.id].value }
                .toSet()
        }

    fun find(id: Int): ResultRow? =
        transaction {
            Namespaces.selectAll().where { Namespaces.id eq id }.firstOrNull()
        }

    fun findByName(name: String): ResultRow? =
        transaction {
            Namespaces.selectAll().where { Namespaces.name eq name }.firstOrNull()
        }

    fun all(): List<ResultRow> =
        transaction {
            Namespaces.selectAll().toList()
        }

    fun delete(id: Int): NamespaceDeleteResult {
        if (!TodoScopeManager.canDeleteNamespace(id)) return NamespaceDeleteResult.BLOCKED_SCOPE
        if (TagManager.byNamespace(id).isNotEmpty()) return NamespaceDeleteResult.BLOCKED_HAS_TAGS
        transaction { Namespaces.deleteWhere { Namespaces.id eq id } }
        return NamespaceDeleteResult.DELETED
    }
}
