package dev.cypdashuhn.worldtasker.db

import dev.rooster.core.YmlOperations
import dev.rooster.core.YmlShell
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

private const val CONFIG_KEY = "todo-scope-namespace"

sealed class TodoResolveResult {
    data class Found(
        val id: Int,
        val name: String
    ) : TodoResolveResult()

    object NotFound : TodoResolveResult()

    data class Ambiguous(
        val todoName: String,
        val scopedOptions: List<String>,
        val hasUntagged: Boolean,
    ) : TodoResolveResult()
}

object TodoScopeManager : YmlOperations by YmlShell("config.yml") {
    private var namespaceId: Int? = null
    private val tags: MutableMap<Int, String> = mutableMapOf() // tagId → name

    fun load() {
        if (!config.contains(CONFIG_KEY)) changeConfig { config.set(CONFIG_KEY, "") }
        val configured = config.getString(CONFIG_KEY)?.takeIf { it.isNotBlank() } ?: return
        val ns = NamespaceManager.findByName(configured)
        if (ns == null) {
            Bukkit.getLogger().warning("[WorldTasker] todo-scope-namespace '$configured' not found — todo scope resolution disabled.")
            return
        }
        namespaceId = ns[NamespaceManager.Namespaces.id].value
        reload()
    }

    private fun reload() {
        val nsId = namespaceId ?: return
        tags.clear()
        TagManager.byNamespace(nsId).forEach { row ->
            tags[row[TagManager.Tags.id].value] = row[TagManager.Tags.name]
        }
    }

    fun isActive(): Boolean = namespaceId != null

    fun isScopeNamespace(id: Int): Boolean = id == namespaceId

    fun isScopeTag(id: Int): Boolean = id in tags

    fun scopeTags(): Map<Int, String> = tags.toMap()

    // ── tag hooks ──────────────────────────────────────────────────────────

    fun onTagCreated(tagId: Int, tagNamespaceId: Int, name: String) {
        if (tagNamespaceId == namespaceId) tags[tagId] = name
    }

    fun onTagRenamed(tagId: Int, newName: String) {
        if (tagId in tags) tags[tagId] = newName
    }

    fun onTagDeleted(tagId: Int) {
        tags.remove(tagId)
    }

    // ── namespace hooks ────────────────────────────────────────────────────

    fun onNamespaceRenamed(id: Int, newName: String) {
        if (id != namespaceId) return
        changeConfig { config.set(CONFIG_KEY, newName) }
    }

    fun canDeleteNamespace(id: Int): Boolean = id != namespaceId

    // ── resolution ────────────────────────────────────────────────────────

    fun resolveInput(key: NamespacedKey, allowedStates: Set<TodoState>): TodoResolveResult {
        val todoName = key.key
        val isBare = key.namespace == "minecraft"
        val isExplicitNoNamespace = key.namespace == "no-namespace"

        val candidates = TodoManager
            .findAllByName(todoName)
            .filter { row -> TodoManager.stateOf(row[TodoManager.Todos.id].value) in allowedStates }

        if (isExplicitNoNamespace) {
            val unscoped = candidates.filter { row ->
                scopeTagNameForTodo(row[TodoManager.Todos.id].value) == null
            }
            return when {
                unscoped.isEmpty() -> TodoResolveResult.NotFound
                else -> TodoResolveResult.Found(unscoped[0][TodoManager.Todos.id].value, todoName)
            }
        }

        if (!isBare) {
            val scoped = candidates.filter { row ->
                scopeTagNameForTodo(row[TodoManager.Todos.id].value) == key.namespace
            }
            return when {
                scoped.isEmpty() -> TodoResolveResult.NotFound
                else -> TodoResolveResult.Found(scoped[0][TodoManager.Todos.id].value, todoName)
            }
        }

        return when (candidates.size) {
            0 -> {
                TodoResolveResult.NotFound
            }

            1 -> {
                TodoResolveResult.Found(candidates[0][TodoManager.Todos.id].value, todoName)
            }

            else -> {
                val scopedOptions = candidates.mapNotNull { row ->
                    val id = row[TodoManager.Todos.id].value
                    scopeTagNameForTodo(id)?.let { tag -> "$tag:$todoName" }
                }
                val hasUntagged = candidates.any { row ->
                    scopeTagNameForTodo(row[TodoManager.Todos.id].value) == null
                }
                TodoResolveResult.Ambiguous(todoName, scopedOptions, hasUntagged)
            }
        }
    }

    fun scopeTagNameForTodo(todoId: Int): String? {
        if (!isActive() || tags.isEmpty()) return null
        return transaction {
            TagManager.TodoTags
                .selectAll()
                .where { TagManager.TodoTags.todoId eq todoId }
                .firstNotNullOfOrNull { row -> tags[row[TagManager.TodoTags.tagId].value] }
        }
    }
}
