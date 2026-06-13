package dev.cypdashuhn.worldtasker.db

import com.google.gson.Gson
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class ProfileData(
    val id: Int,
    val name: String,
    val filter: TodoFilter,
)

sealed class ProfileSaveResult {
    object Saved : ProfileSaveResult()
    object DuplicateName : ProfileSaveResult()
}

object QueryProfileManager {
    object QueryProfiles : IntIdTable() {
        val name = varchar("name", 64).uniqueIndex()
        val filterJson = text("filter_json")
    }

    private val gson = Gson()

    fun save(name: String, filter: TodoFilter): ProfileSaveResult {
        if (findByName(name) != null) return ProfileSaveResult.DuplicateName
        transaction {
            QueryProfiles.insert {
                it[QueryProfiles.name] = name
                it[QueryProfiles.filterJson] = gson.toJson(filter)
            }
        }
        return ProfileSaveResult.Saved
    }

    fun overwrite(name: String, filter: TodoFilter) {
        val existing = findByName(name)
        if (existing != null) {
            transaction {
                QueryProfiles.update({ QueryProfiles.id eq existing[QueryProfiles.id].value }) {
                    it[QueryProfiles.filterJson] = gson.toJson(filter)
                }
            }
        } else {
            transaction {
                QueryProfiles.insert {
                    it[QueryProfiles.name] = name
                    it[QueryProfiles.filterJson] = gson.toJson(filter)
                }
            }
        }
    }

    fun rename(id: Int, newName: String): Boolean {
        if (findByName(newName) != null) return false
        transaction {
            QueryProfiles.update({ QueryProfiles.id eq id }) {
                it[QueryProfiles.name] = newName
            }
        }
        return true
    }

    fun delete(id: Int) {
        transaction { QueryProfiles.deleteWhere { QueryProfiles.id eq id } }
    }

    fun findByName(name: String): ResultRow? =
        transaction { QueryProfiles.selectAll().where { QueryProfiles.name eq name }.firstOrNull() }

    fun all(): List<ProfileData> =
        transaction {
            QueryProfiles.selectAll().map {
                ProfileData(
                    id = it[QueryProfiles.id].value,
                    name = it[QueryProfiles.name],
                    filter = gson.fromJson(it[QueryProfiles.filterJson], TodoFilter::class.java),
                )
            }
        }
}
