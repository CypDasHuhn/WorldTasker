package dev.cypdashuhn.worldtasker.actions

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import org.bukkit.entity.Player

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

    fun remove(sender: Player, tagName: String) {
        val tag = TagManager.findByQualifiedName(tagName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$tagName</white>' not found.")
            return
        }
        TagManager.delete(tag[TagManager.Tags.id].value)
        sender.msg("<green>Tag '<white>$tagName</white>' removed.")
    }

    fun rename(sender: Player, oldName: String, newName: String) {
        val tag = TagManager.findByQualifiedName(oldName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$oldName</white>' not found.")
            return
        }
        TagManager.rename(tag[TagManager.Tags.id].value, newName)
        sender.msg("<green>Tag renamed to '<white>$newName</white>'.")
    }

    fun addInheritance(sender: Player, childName: String, parentName: String) {
        val child = TagManager.findByQualifiedName(childName)
        val parent = TagManager.findByQualifiedName(parentName)
        if (child == null) { sender.msg("<red>Tag '<white>$childName</white>' not found."); return }
        if (parent == null) { sender.msg("<red>Tag '<white>$parentName</white>' not found."); return }
        TagManager.addInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>$childName</white>' now inherits from '<white>$parentName</white>'.")
    }

    fun setInheritance(sender: Player, childName: String, parentsStr: String) {
        val child = TagManager.findByQualifiedName(childName)
        if (child == null) { sender.msg("<red>Tag '<white>$childName</white>' not found."); return }
        TagManager.setInheritance(child[TagManager.Tags.id].value, resolveTagIds(parentsStr, sender))
        sender.msg("<green>Inheritance for '<white>$childName</white>' updated.")
    }

    fun removeInheritance(sender: Player, childName: String, parentName: String) {
        val child = TagManager.findByQualifiedName(childName)
        val parent = TagManager.findByQualifiedName(parentName)
        if (child == null) { sender.msg("<red>Tag '<white>$childName</white>' not found."); return }
        if (parent == null) { sender.msg("<red>Tag '<white>$parentName</white>' not found."); return }
        TagManager.removeInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>$childName</white>' no longer inherits from '<white>$parentName</white>'.")
    }
}
