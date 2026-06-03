package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

//region node names
private const val ADD_NAME    = "todoNsAddName"
private const val REMOVE_NAME = "todoNsRemoveName"
private const val RENAME_OLD  = "todoNsRenameOld"
private const val RENAME_NEW  = "todoNsRenameNew"
private const val INFO_NAME   = "todoNsInfoName"
//endregion

internal fun buildNamespacesNode(): LiteralArgument {
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

    val nsAddArg = StringArgument(ADD_NAME)
    nsAddArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[ADD_NAME] as String
        NamespaceManager.create(name)
        sender.msg("<green>Namespace '<white>$name</white>' created.")
    })
    nsNode.then(LiteralArgument("add").then(nsAddArg))

    val nsRemoveArg = StringArgument(REMOVE_NAME).suggestNamespaceNames()
    nsRemoveArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[REMOVE_NAME] as String
        val ns = NamespaceManager.findByName(name)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }
        NamespaceManager.delete(ns[NamespaceManager.Namespaces.id].value)
        sender.msg("<green>Namespace '<white>$name</white>' removed.")
    })
    nsNode.then(LiteralArgument("remove").then(nsRemoveArg))

    val nsRenameOldArg = StringArgument(RENAME_OLD).suggestNamespaceNames()
    val nsRenameNewArg = StringArgument(RENAME_NEW)
    nsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val oldName = args.argsMap[RENAME_OLD] as String
        val newName = args.argsMap[RENAME_NEW] as String
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

    val nsInfoArg = StringArgument(INFO_NAME).suggestNamespaceNames()
    nsInfoArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[INFO_NAME] as String
        val ns = NamespaceManager.findByName(name)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }
        val nsId = ns[NamespaceManager.Namespaces.id].value
        val tags = TagManager.byNamespace(nsId)
        if (tags.isEmpty()) {
            sender.msg("<gold>=== $name ===")
            sender.msg("<gray>No tags in this namespace.")
            return@PlayerCommandExecutor
        }
        sender.msg("<gold>=== $name ===")
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
                    if (parentNsId == nsId) parentTagName
                    else {
                        val parentNsName = NamespaceManager.find(parentNsId)?.get(NamespaceManager.Namespaces.name) ?: "?"
                        "$parentNsName:$parentTagName"
                    }
                }
                sender.msg("<white>$tagName <dark_gray>inherits: <white>$parentLabels")
            }
        }
    })
    nsNode.then(LiteralArgument("info").then(nsInfoArg))

    return nsNode
}
