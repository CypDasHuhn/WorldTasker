package dev.cypdashuhn.worldtasker.actions

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

private val VALID_NAME = Regex("[a-z0-9_\\-]+")
fun isValidResourceName(name: String) = name.matches(VALID_NAME)
private fun Player.invalidName(name: String) =
    msg("<red>'<white>$name</white>' is invalid. Use only lowercase letters, numbers, '_', '-'.")

fun resolveTagIds(tagsStr: String, sender: Player): List<Int> =
    tagsStr.split(",").mapNotNull { raw ->
        val name = raw.trim()
        if (name.isEmpty()) return@mapNotNull null
        val tag = TagManager.findByQualifiedName(name)
        if (tag == null) {
            sender.msg("<yellow>Tag '<white>$name</white>' not found, skipping.")
            null
        } else {
            tag[TagManager.Tags.id].value
        }
    }

object TagActions {
    fun list(sender: Player) {
        val tags = TagManager.all()
        if (tags.isEmpty()) {
            sender.msg("<gray>No tags found.")
            return
        }
        sender.msg("<gold>=== Tags ===")
        tags.forEach { row ->
            val nsId = row[TagManager.Tags.namespaceId].value
            val ns = NamespaceManager.find(nsId)?.get(NamespaceManager.Namespaces.name) ?: "?"
            sender.msg("<yellow>$ns<gray>:<white>${row[TagManager.Tags.name]}")
        }
    }

    fun add(sender: Player, nsName: String, tagName: String) {
        if (!isValidResourceName(tagName)) { sender.invalidName(tagName); return }
        val ns = NamespaceManager.findByName(nsName)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$nsName</white>' not found. Create it first with /todo tags namespaces add.")
            return
        }
        val nsId = ns[NamespaceManager.Namespaces.id].value
        if (TagManager.findByName(tagName, nsId) != null) {
            sender.msg("<red>Tag '<white>$nsName:$tagName</white>' already exists.")
            return
        }
        TagManager.create(tagName, nsId)
        sender.msg("<green>Tag '<white>$nsName:$tagName</white>' created.")
    }

    fun remove(sender: Player, key: NamespacedKey) {
        val tag = TagManager.findByQualifiedName(key)
        if (tag == null) {
            sender.msg("<red>Tag '<white>${key.namespace}:${key.key}</white>' not found.")
            return
        }
        TagManager.delete(tag[TagManager.Tags.id].value)
        sender.msg("<green>Tag '<white>${key.namespace}:${key.key}</white>' removed.")
    }

    fun rename(sender: Player, oldKey: NamespacedKey, newName: String) {
        if (!isValidResourceName(newName)) { sender.invalidName(newName); return }
        val tag = TagManager.findByQualifiedName(oldKey)
        if (tag == null) {
            sender.msg("<red>Tag '<white>${oldKey.namespace}:${oldKey.key}</white>' not found.")
            return
        }
        TagManager.rename(tag[TagManager.Tags.id].value, newName)
        sender.msg("<green>Tag renamed to '<white>$newName</white>'.")
    }

    fun addInheritance(sender: Player, childKey: NamespacedKey, parentKey: NamespacedKey) {
        val child = TagManager.findByQualifiedName(childKey)
        val parent = TagManager.findByQualifiedName(parentKey)
        if (child == null) { sender.msg("<red>Tag '<white>${childKey.namespace}:${childKey.key}</white>' not found."); return }
        if (parent == null) { sender.msg("<red>Tag '<white>${parentKey.namespace}:${parentKey.key}</white>' not found."); return }
        TagManager.addInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>${childKey.namespace}:${childKey.key}</white>' now inherits from '<white>${parentKey.namespace}:${parentKey.key}</white>'.")
    }

    fun setInheritance(sender: Player, childKey: NamespacedKey, parentsStr: String) {
        val child = TagManager.findByQualifiedName(childKey)
        if (child == null) { sender.msg("<red>Tag '<white>${childKey.namespace}:${childKey.key}</white>' not found."); return }
        TagManager.setInheritance(child[TagManager.Tags.id].value, resolveTagIds(parentsStr, sender))
        sender.msg("<green>Inheritance for '<white>${childKey.namespace}:${childKey.key}</white>' updated.")
    }

    fun removeInheritance(sender: Player, childKey: NamespacedKey, parentKey: NamespacedKey) {
        val child = TagManager.findByQualifiedName(childKey)
        val parent = TagManager.findByQualifiedName(parentKey)
        if (child == null) { sender.msg("<red>Tag '<white>${childKey.namespace}:${childKey.key}</white>' not found."); return }
        if (parent == null) { sender.msg("<red>Tag '<white>${parentKey.namespace}:${parentKey.key}</white>' not found."); return }
        TagManager.removeInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>${childKey.namespace}:${childKey.key}</white>' no longer inherits from '<white>${parentKey.namespace}:${parentKey.key}</white>'.")
    }
}
