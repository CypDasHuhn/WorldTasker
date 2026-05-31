package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

internal fun buildTodoTagsNode(): LiteralArgument {
    val tagsNode = LiteralArgument("tags")

    tagsNode.then(
        LiteralArgument("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            val tags = TagManager.all()
            if (tags.isEmpty()) {
                sender.msg("<gray>No tags found.")
                return@PlayerCommandExecutor
            }
            sender.msg("<gold>=== Tags ===")
            tags.forEach { row ->
                val nsId = row[TagManager.Tags.namespaceId].value
                val ns = NamespaceManager.find(nsId)?.get(NamespaceManager.Namespaces.name) ?: "?"
                sender.msg("<yellow>$ns<gray>:<white>${row[TagManager.Tags.name]}")
            }
        })
    )

    val tagsAddNsArg = StringArgument("todoTagsAddNs").suggestNamespaceNames()
    val tagsAddNameArg = StringArgument("todoTagsAddName")
    tagsAddNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val nsName = args.argsMap["todoTagsAddNs"] as String
        val tagName = args.argsMap["todoTagsAddName"] as String
        val ns = NamespaceManager.findByName(nsName)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$nsName</white>' not found. Create it first with /todo tags namespaces add.")
            return@PlayerCommandExecutor
        }
        val nsId = ns[NamespaceManager.Namespaces.id].value
        if (TagManager.findByName(tagName, nsId) != null) {
            sender.msg("<red>Tag '<white>$nsName:$tagName</white>' already exists.")
            return@PlayerCommandExecutor
        }
        TagManager.create(tagName, nsId)
        sender.msg("<green>Tag '<white>$nsName:$tagName</white>' created.")
    })
    tagsAddNsArg.then(tagsAddNameArg)
    tagsNode.then(LiteralArgument("add").then(tagsAddNsArg))

    val tagsRemoveNameArg = StringArgument("todoTagsRemoveName").suggestTagNames()
    tagsRemoveNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val tagName = args.argsMap["todoTagsRemoveName"] as String
        val tag = TagManager.findByName(tagName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$tagName</white>' not found.")
            return@PlayerCommandExecutor
        }
        TagManager.delete(tag[TagManager.Tags.id].value)
        sender.msg("<green>Tag '<white>$tagName</white>' removed.")
    })
    tagsNode.then(LiteralArgument("remove").then(tagsRemoveNameArg))

    val tagsRenameOldArg = StringArgument("todoTagsRenameOld").suggestTagNames()
    val tagsRenameNewArg = StringArgument("todoTagsRenameNew")
    tagsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val oldName = args.argsMap["todoTagsRenameOld"] as String
        val newName = args.argsMap["todoTagsRenameNew"] as String
        val tag = TagManager.findByName(oldName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$oldName</white>' not found.")
            return@PlayerCommandExecutor
        }
        TagManager.rename(tag[TagManager.Tags.id].value, newName)
        sender.msg("<green>Tag renamed to '<white>$newName</white>'.")
    })
    tagsRenameOldArg.then(tagsRenameNewArg)
    tagsNode.then(LiteralArgument("rename").then(tagsRenameOldArg))

    tagsNode.then(buildNamespacesNode())

    return tagsNode
}

private fun buildNamespacesNode(): LiteralArgument {
    val nsNode = LiteralArgument("namespaces")

    nsNode.then(
        LiteralArgument("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            val namespaces = NamespaceManager.all()
            if (namespaces.isEmpty()) {
                sender.msg("<gray>No namespaces found.")
                return@PlayerCommandExecutor
            }
            sender.msg("<gold>=== Namespaces ===")
            namespaces.forEach { sender.msg("<white>${it[NamespaceManager.Namespaces.name]}") }
        })
    )

    val nsAddArg = StringArgument("todoNsAddName")
    nsAddArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap["todoNsAddName"] as String
        NamespaceManager.create(name)
        sender.msg("<green>Namespace '<white>$name</white>' created.")
    })
    nsNode.then(LiteralArgument("add").then(nsAddArg))

    val nsRemoveArg = StringArgument("todoNsRemoveName").suggestNamespaceNames()
    nsRemoveArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap["todoNsRemoveName"] as String
        val ns = NamespaceManager.findByName(name)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }
        NamespaceManager.delete(ns[NamespaceManager.Namespaces.id].value)
        sender.msg("<green>Namespace '<white>$name</white>' removed.")
    })
    nsNode.then(LiteralArgument("remove").then(nsRemoveArg))

    val nsRenameOldArg = StringArgument("todoNsRenameOld").suggestNamespaceNames()
    val nsRenameNewArg = StringArgument("todoNsRenameNew")
    nsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val oldName = args.argsMap["todoNsRenameOld"] as String
        val newName = args.argsMap["todoNsRenameNew"] as String
        val ns = NamespaceManager.findByName(oldName)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$oldName</white>' not found.")
            return@PlayerCommandExecutor
        }
        NamespaceManager.rename(ns[NamespaceManager.Namespaces.id].value, newName)
        sender.msg("<green>Namespace renamed to '<white>$newName</white>'.")
    })
    nsRenameOldArg.then(nsRenameNewArg)
    nsNode.then(LiteralArgument("rename").then(nsRenameOldArg))

    return nsNode
}
