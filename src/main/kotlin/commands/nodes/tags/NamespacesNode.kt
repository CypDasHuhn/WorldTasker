package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.actions.NamespaceActions
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val ADD_NAME    = "todoNsAddName"
private const val REMOVE_NAME = "todoNsRemoveName"
private const val RENAME_OLD  = "todoNsRenameOld"
private const val RENAME_NEW  = "todoNsRenameNew"
private const val INFO_NAME   = "todoNsInfoName"

internal fun buildNamespacesNode(): LiteralArgument {
    val nsNode = LiteralArgument("namespaces")

    nsNode.then(
        LiteralArgument("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            NamespaceActions.list(sender)
        })
    )

    val nsAddArg = StringArgument(ADD_NAME)
    nsAddArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        NamespaceActions.add(sender, args.argsMap[ADD_NAME] as String)
    })
    nsNode.then(LiteralArgument("add").then(nsAddArg))

    val nsRemoveArg = StringArgument(REMOVE_NAME).suggestNamespaceNames()
    nsRemoveArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        NamespaceActions.remove(sender, args.argsMap[REMOVE_NAME] as String)
    })
    nsNode.then(LiteralArgument("remove").then(nsRemoveArg))

    val nsRenameOldArg = StringArgument(RENAME_OLD).suggestNamespaceNames()
    val nsRenameNewArg = StringArgument(RENAME_NEW)
    nsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        NamespaceActions.rename(sender, args.argsMap[RENAME_OLD] as String, args.argsMap[RENAME_NEW] as String)
    })
    nsRenameOldArg.then(nsRenameNewArg)
    nsNode.then(LiteralArgument("rename").then(nsRenameOldArg))

    val nsInfoArg = StringArgument(INFO_NAME).suggestNamespaceNames()
    nsInfoArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        NamespaceActions.info(sender, args.argsMap[INFO_NAME] as String)
    })
    nsNode.then(LiteralArgument("info").then(nsInfoArg))

    return nsNode
}
