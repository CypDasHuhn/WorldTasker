package dev.cypdashuhn.worldtasker.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object TodoManager {
    object Todos : IntIdTable() {
        val name = text("name")
        val author = text("author")
        val description = text("description")
        val tags = text("tags")
        val createdAt = datetime("created_at")
        val updatedAt = datetime("updated_at")
    }
}
