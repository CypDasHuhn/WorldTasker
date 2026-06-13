package dev.cypdashuhn.worldtasker.actions

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceCreateResult
import dev.cypdashuhn.worldtasker.db.NamespaceDeleteResult
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.NamespaceRenameResult
import dev.cypdashuhn.worldtasker.db.TagManager
import org.bukkit.entity.Player

private fun Player.invalidName(name: String) =
    msg("<red>'<white>$name</white>' is invalid. Use only lowercase letters, numbers, '_', '.', '-'.")

object NamespaceActions {
    fun list(sender: Player) {
        val namespaces = NamespaceManager.all()
        if (namespaces.isEmpty()) { sender.msg("<gray>No namespaces found."); return }
        sender.msg("<gold>=== Namespaces ===")
        namespaces.forEach { sender.msg("<white>${it[NamespaceManager.Namespaces.name]}") }
    }

    fun add(sender: Player, name: String, allowsMultiple: Boolean = true) {
        if (!isValidResourceName(name)) { sender.invalidName(name); return }
        when (NamespaceManager.create(name, allowsMultiple)) {
            is NamespaceCreateResult.Created -> {
                val mode = if (allowsMultiple) "multiple" else "single"
                sender.msg("<green>Namespace '<white>$name</white>' created ($mode tag).")
            }
            NamespaceCreateResult.ReservedName -> sender.reservedName(name)
            NamespaceCreateResult.DuplicateName -> sender.msg("<red>Namespace '<white>$name</white>' already exists.")
        }
    }

    fun remove(sender: Player, name: String) {
        val ns = NamespaceManager.findByName(name)
        if (ns == null) { sender.msg("<red>Namespace '<white>$name</white>' not found."); return }
        when (NamespaceManager.delete(ns[NamespaceManager.Namespaces.id].value)) {
            NamespaceDeleteResult.DELETED -> sender.msg("<green>Namespace '<white>$name</white>' removed.")
            NamespaceDeleteResult.BLOCKED_SCOPE ->
                sender.msg("<red>Namespace '<white>$name</white>' is the active todo scope and cannot be deleted.")
            NamespaceDeleteResult.BLOCKED_HAS_TAGS ->
                sender.msg("<red>Namespace '<white>$name</white>' still has tags. Remove all tags first.")
        }
    }

    fun rename(sender: Player, oldName: String, newName: String) {
        if (!isValidResourceName(newName)) { sender.invalidName(newName); return }
        val ns = NamespaceManager.findByName(oldName)
        if (ns == null) { sender.msg("<red>Namespace '<white>$oldName</white>' not found."); return }
        when (NamespaceManager.rename(ns[NamespaceManager.Namespaces.id].value, newName)) {
            NamespaceRenameResult.RENAMED -> sender.msg("<green>Namespace renamed to '<white>$newName</white>'.")
            NamespaceRenameResult.RESERVED_NAME -> sender.reservedName(newName)
            NamespaceRenameResult.DUPLICATE_NAME ->
                sender.msg("<red>Namespace '<white>$newName</white>' already exists.")
        }
    }

    fun info(sender: Player, name: String) {
        val ns = NamespaceManager.findByName(name)
        if (ns == null) { sender.msg("<red>Namespace '<white>$name</white>' not found."); return }
        val nsId = ns[NamespaceManager.Namespaces.id].value
        val allowsMultiple = ns[NamespaceManager.Namespaces.allowsMultiple]
        val tags = TagManager.byNamespace(nsId)
        sender.msg("<gold>=== $name ===")
        sender.msg("<gray>Tag mode: <white>${if (allowsMultiple) "multiple" else "single"}")
        if (tags.isEmpty()) { sender.msg("<gray>No tags in this namespace."); return }
        tags.forEach { row ->
            val tagId = row[TagManager.Tags.id].value
            val tagName = row[TagManager.Tags.name]
            val parents = TagManager.parentsOf(tagId)
            if (parents.isEmpty()) {
                sender.msg("<white>$tagName")
            } else {
                val parentLabels = parents.joinToString("<gray>, <white>") { parent ->
                    val parentNsId = parent[TagManager.Tags.namespaceId].value
                    val parentTagName = parent[TagManager.Tags.name]
                    if (parentNsId == nsId) {
                        parentTagName
                    } else {
                        val parentNsName =
                            NamespaceManager.find(parentNsId)?.get(NamespaceManager.Namespaces.name) ?: "?"
                        "$parentNsName:$parentTagName"
                    }
                }
                sender.msg("<white>$tagName <dark_gray>inherits: <white>$parentLabels")
            }
        }
    }
}
