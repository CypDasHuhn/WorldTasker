package dev.cypdashuhn.worldtasker.db

import dev.rooster.core.YmlOperations
import dev.rooster.core.YmlShell
import org.bukkit.Bukkit

private const val CONFIG_KEY = "todo-scope-namespace"

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
}
