package dev.cypdashuhn.worldtasker.db

import org.bukkit.NamespacedKey
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

sealed class TagCreateResult {
    data class Created(
        val id: Int
    ) : TagCreateResult()

    object ReservedName : TagCreateResult()

    object DuplicateName : TagCreateResult()
}

enum class TagRenameResult { RENAMED, RESERVED_NAME, DUPLICATE_NAME }

sealed class TagAssignResult {
    object Success : TagAssignResult()

    object MultipleScopeTags : TagAssignResult()

    data class ScopeCollision(
        val todoName: String,
        val scopeTagName: String?
    ) : TagAssignResult()

    data class NamespaceSingleTagViolation(
        val namespaceNames: List<String>
    ) : TagAssignResult()
}

object TagManager {
    object Tags : IntIdTable() {
        val name = varchar("name", 64)
        val namespaceId = reference("namespace_id", NamespaceManager.Namespaces)
        val material = varchar("material", 64).default("PAPER")
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

    // ── scope constraint helpers ───────────────────────────────────────────

    private fun validateScopeConstraints(todoId: Int, tagIds: List<Int>): TagAssignResult {
        if (!TodoScopeManager.isActive()) return TagAssignResult.Success
        if (TodoScopeManager.countScopeTagsAmong(tagIds) > 1) return TagAssignResult.MultipleScopeTags
        val scopeTagName = TodoScopeManager.scopeTagNameAmong(tagIds)
        val todoName = TodoManager.findById(todoId)?.name ?: return TagAssignResult.Success
        return if (TodoScopeManager.wouldCollide(todoName, scopeTagName, excludeTodoId = todoId)) {
            TagAssignResult.ScopeCollision(todoName, scopeTagName)
        } else {
            TagAssignResult.Success
        }
    }

    private fun validateSingleTagNamespaces(tagIds: List<Int>): TagAssignResult {
        val singleTagNsIds = NamespaceManager.singleTagNamespaceIds()
        if (singleTagNsIds.isEmpty()) return TagAssignResult.Success
        val tagNsMap = transaction {
            Tags
                .selectAll()
                .where { Tags.id inList tagIds }
                .groupBy { it[Tags.namespaceId].value }
        }
        val violatingNamespaces = mutableListOf<String>()
        for ((nsId, tags) in tagNsMap) {
            if (nsId in singleTagNsIds && tags.size > 1) {
                val nsName = NamespaceManager.find(nsId)?.get(NamespaceManager.Namespaces.name) ?: "?"
                violatingNamespaces.add(nsName)
            }
        }
        return if (violatingNamespaces.isNotEmpty()) {
            TagAssignResult.NamespaceSingleTagViolation(violatingNamespaces)
        } else {
            TagAssignResult.Success
        }
    }

    private fun validateTagAssignments(todoId: Int, tagIds: List<Int>): TagAssignResult {
        val s = validateScopeConstraints(todoId, tagIds)
        if (s != TagAssignResult.Success) return s
        return validateSingleTagNamespaces(tagIds)
    }

    // ── tag CRUD ───────────────────────────────────────────────────────────

    fun create(name: String, namespaceId: Int): TagCreateResult {
        if (name in RESERVED_NAMES) return TagCreateResult.ReservedName
        if (findByName(name, namespaceId) != null) return TagCreateResult.DuplicateName
        val id = transaction {
            Tags
                .insert {
                    it[Tags.name] = name
                    it[Tags.namespaceId] = namespaceId
                }[Tags.id]
                .value
        }
        TodoScopeManager.onTagCreated(id, namespaceId, name)
        return TagCreateResult.Created(id)
    }

    fun rename(id: Int, name: String): TagRenameResult {
        if (name in RESERVED_NAMES) return TagRenameResult.RESERVED_NAME
        val tag = find(id) ?: return TagRenameResult.RENAMED
        val nsId = tag[Tags.namespaceId].value
        val existing = findByName(name, nsId)
        if (existing != null && existing[Tags.id].value != id) return TagRenameResult.DUPLICATE_NAME
        transaction { Tags.update({ Tags.id eq id }) { it[Tags.name] = name } }
        TodoScopeManager.onTagRenamed(id, name)
        return TagRenameResult.RENAMED
    }

    fun find(id: Int): ResultRow? = transaction { Tags.selectAll().where { Tags.id eq id }.firstOrNull() }

    fun findByName(name: String, namespaceId: Int): ResultRow? =
        transaction {
            Tags.selectAll().where { (Tags.name eq name) and (Tags.namespaceId eq namespaceId) }.firstOrNull()
        }

    fun findByName(name: String): ResultRow? = transaction { Tags.selectAll().where { Tags.name eq name }.firstOrNull() }

    fun findByQualifiedName(key: NamespacedKey): ResultRow? {
        val ns = NamespaceManager.findByName(key.namespace) ?: return null
        return findByName(key.key, ns[NamespaceManager.Namespaces.id].value)
    }

    fun findByQualifiedName(qualified: String): ResultRow? {
        val colon = qualified.indexOf(':')
        if (colon == -1) return null
        val ns = NamespaceManager.findByName(qualified.substring(0, colon)) ?: return null
        return findByName(qualified.substring(colon + 1), ns[NamespaceManager.Namespaces.id].value)
    }

    fun all(): List<ResultRow> = transaction { Tags.selectAll().toList() }

    fun byNamespace(namespaceId: Int): List<ResultRow> = transaction { Tags.selectAll().where { Tags.namespaceId eq namespaceId }.toList() }

    fun updateMaterial(id: Int, material: String) = transaction { Tags.update({ Tags.id eq id }) { it[Tags.material] = material } }

    fun delete(id: Int) {
        transaction {
            TodoTags.deleteWhere { TodoTags.tagId eq id }
            TagInheritance.deleteWhere { TagInheritance.childId eq id }
            TagInheritance.deleteWhere { TagInheritance.parentId eq id }
            Tags.deleteWhere { Tags.id eq id }
        }
        TodoScopeManager.onTagDeleted(id)
    }

    // ── todo-tag assignment ────────────────────────────────────────────────

    fun addToTodo(todoId: Int, tagId: Int): TagAssignResult =
        transaction {
            val currentTagIds = tagsForTodo(todoId).map { it[Tags.id].value }
            val combinedIds = (currentTagIds + tagId).distinct()
            val check = validateTagAssignments(todoId, combinedIds)
            if (check != TagAssignResult.Success) return@transaction check
            if (tagId !in currentTagIds) {
                TodoTags.insert {
                    it[TodoTags.todoId] = todoId
                    it[TodoTags.tagId] = tagId
                }
            }
            TagAssignResult.Success
        }

    fun addTagsToTodo(todoId: Int, tagIds: List<Int>): TagAssignResult =
        transaction {
            val currentTagIds = tagsForTodo(todoId).map { it[Tags.id].value }
            val combinedIds = (currentTagIds + tagIds).distinct()
            val check = validateTagAssignments(todoId, combinedIds)
            if (check != TagAssignResult.Success) return@transaction check
            tagIds.forEach { tagId ->
                if (tagId !in currentTagIds) {
                    TodoTags.insert {
                        it[TodoTags.todoId] = todoId
                        it[TodoTags.tagId] = tagId
                    }
                }
            }
            TagAssignResult.Success
        }

    fun setTagsForTodo(todoId: Int, tagIds: List<Int>): TagAssignResult =
        transaction {
            val distinctIds = tagIds.distinct()
            val check = validateTagAssignments(todoId, distinctIds)
            if (check != TagAssignResult.Success) return@transaction check
            TodoTags.deleteWhere { TodoTags.todoId eq todoId }
            distinctIds.forEach { tagId ->
                TodoTags.insert {
                    it[TodoTags.todoId] = todoId
                    it[TodoTags.tagId] = tagId
                }
            }
            TagAssignResult.Success
        }

    fun removeTagsFromTodo(todoId: Int, tagIds: List<Int>): TagAssignResult =
        transaction {
            val currentTagIds = tagsForTodo(todoId).map { it[Tags.id].value }
            val remainingTagIds = (currentTagIds - tagIds.toSet()).distinct()
            val check = validateTagAssignments(todoId, remainingTagIds)
            if (check != TagAssignResult.Success) return@transaction check
            tagIds.distinct().forEach { tagId ->
                TodoTags.deleteWhere { (TodoTags.todoId eq todoId) and (TodoTags.tagId eq tagId) }
            }
            TagAssignResult.Success
        }

    fun removeAllForTodo(todoId: Int) = transaction { TodoTags.deleteWhere { TodoTags.todoId eq todoId } }

    fun tagsForTodo(todoId: Int): List<ResultRow> =
        transaction {
            (Tags innerJoin TodoTags).selectAll().where { TodoTags.todoId eq todoId }.toList()
        }

    fun tagLabelsForTodo(todoId: Int): List<String> =
        transaction {
            (Tags innerJoin TodoTags innerJoin NamespaceManager.Namespaces)
                .selectAll()
                .where { TodoTags.todoId eq todoId }
                .map { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
        }

    fun todosForTag(tagId: Int): List<ResultRow> =
        transaction {
            (TodoManager.Todos innerJoin TodoTags).selectAll().where { TodoTags.tagId eq tagId }.toList()
        }

    // ── inheritance ────────────────────────────────────────────────────────

    fun addInheritance(childId: Int, parentId: Int) =
        transaction {
            val exists = TagInheritance
                .selectAll()
                .where { (TagInheritance.childId eq childId) and (TagInheritance.parentId eq parentId) }
                .firstOrNull() != null
            if (!exists) {
                TagInheritance.insert {
                    it[TagInheritance.childId] = childId
                    it[TagInheritance.parentId] = parentId
                }
            }
        }

    fun removeInheritance(childId: Int, parentId: Int) =
        transaction {
            TagInheritance.deleteWhere {
                (TagInheritance.childId eq childId) and (TagInheritance.parentId eq parentId)
            }
        }

    fun setInheritance(childId: Int, parentIds: List<Int>) =
        transaction {
            TagInheritance.deleteWhere { TagInheritance.childId eq childId }
            parentIds.forEach { parentId ->
                TagInheritance.insert {
                    it[TagInheritance.childId] = childId
                    it[TagInheritance.parentId] = parentId
                }
            }
        }

    fun parentsOf(tagId: Int): List<ResultRow> =
        transaction {
            val parentIds = TagInheritance
                .selectAll()
                .where { TagInheritance.childId eq tagId }
                .map { it[TagInheritance.parentId].value }
            if (parentIds.isEmpty()) {
                emptyList()
            } else {
                Tags.selectAll().where { Tags.id inList parentIds }.toList()
            }
        }

    /** BFS expansion: returns the given IDs plus all ancestor IDs, cycle-safe. */
    fun expandTagIds(directIds: Set<Int>): Set<Int> =
        transaction {
            val visited = directIds.toMutableSet()
            val queue = ArrayDeque(directIds.toList())
            while (queue.isNotEmpty()) {
                val tagId = queue.removeFirst()
                TagInheritance
                    .selectAll()
                    .where { TagInheritance.childId eq tagId }
                    .map { it[TagInheritance.parentId].value }
                    .filter { it !in visited }
                    .forEach {
                        visited.add(it)
                        queue.add(it)
                    }
            }
            visited
        }

    fun expandedTagNamesForTodo(todoId: Int): Set<String> =
        transaction {
            val directIds = tagsForTodo(todoId).map { it[Tags.id].value }.toSet()
            val allIds = expandTagIds(directIds)
            if (allIds.isEmpty()) {
                emptySet()
            } else {
                (Tags innerJoin NamespaceManager.Namespaces)
                    .selectAll()
                    .where { Tags.id inList allIds.toList() }
                    .map { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
                    .toSet()
            }
        }

    fun ancestorLabelsOf(tagId: Int): List<String> =
        transaction {
            val directIds = parentsOf(tagId).map { it[Tags.id].value }.toSet()
            val allAncestorIds = expandTagIds(directIds)
            if (allAncestorIds.isEmpty()) return@transaction emptyList()
            val rows = (Tags innerJoin NamespaceManager.Namespaces)
                .selectAll()
                .where { Tags.id inList allAncestorIds.toList() }
                .associateBy { it[Tags.id].value }
            val transitiveIds = allAncestorIds - directIds
            (directIds + transitiveIds).mapNotNull { id ->
                rows[id]?.let { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
            }
        }

    fun inheritedTagLabelsForTodo(todoId: Int): List<String> =
        transaction {
            val directIds = tagsForTodo(todoId).map { it[Tags.id].value }.toSet()
            val inheritedIds = expandTagIds(directIds) - directIds
            if (inheritedIds.isEmpty()) {
                emptyList()
            } else {
                (Tags innerJoin NamespaceManager.Namespaces)
                    .selectAll()
                    .where { Tags.id inList inheritedIds.toList() }
                    .map { "${it[NamespaceManager.Namespaces.name]}:${it[Tags.name]}" }
            }
        }
}
