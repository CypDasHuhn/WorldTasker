package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

enum class NamespaceDeleteResult { DELETED, BLOCKED_SCOPE, BLOCKED_HAS_TAGS }

sealed class NamespaceCreateResult {
    data class Created(val id: Int) : NamespaceCreateResult()
    object ReservedName : NamespaceCreateResult()
    object DuplicateName : NamespaceCreateResult()
}

enum class NamespaceRenameResult { RENAMED, RESERVED_NAME, DUPLICATE_NAME }

object NamespaceManager {
    object Namespaces : IntIdTable() {
        val name = varchar("name", 64).uniqueIndex()
        val material = varchar("material", 64).default("BOOKSHELF")
    }

    fun create(name: String): NamespaceCreateResult {
        if (name in RESERVED_NAMES) return NamespaceCreateResult.ReservedName
        if (findByName(name) != null) return NamespaceCreateResult.DuplicateName
        val id = transaction { Namespaces.insert { it[Namespaces.name] = name }[Namespaces.id].value }
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
