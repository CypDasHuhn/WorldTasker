package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

enum class NamespaceDeleteResult { DELETED, BLOCKED_SCOPE }

object NamespaceManager {
    object Namespaces : IntIdTable() {
        val name = varchar("name", 64).uniqueIndex()
        val material = varchar("material", 64).default("BOOKSHELF")
    }

    fun create(name: String): Int =
        transaction {
            Namespaces.insert { it[Namespaces.name] = name }[Namespaces.id].value
        }

    fun rename(id: Int, name: String) {
        transaction { Namespaces.update({ Namespaces.id eq id }) { it[Namespaces.name] = name } }
        TodoScopeManager.onNamespaceRenamed(id, name)
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
        transaction { Namespaces.deleteWhere { Namespaces.id eq id } }
        return NamespaceDeleteResult.DELETED
    }
}
