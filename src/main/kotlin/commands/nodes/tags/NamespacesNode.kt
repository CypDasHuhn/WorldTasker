package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.actions.NamespaceActions
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val ADD_NAME = "todoNsAddName"
private const val REMOVE_NAME = "todoNsRemoveName"
private const val RENAME_OLD = "todoNsRenameOld"
private const val RENAME_NEW = "todoNsRenameNew"
private const val INFO_NAME = "todoNsInfoName"

internal fun buildNamespacesNode(): LiteralArgument =
    la("namespaces").apply {
        then(la("list").executesPlayer(PlayerCommandExecutor { sender, _ -> NamespaceActions.list(sender) }))
        then(la("add").then(
            StringArgument(ADD_NAME).apply {
                then(la("--single").executesPlayer(PlayerCommandExecutor { sender, args ->
                    NamespaceActions.add(sender, args.argsMap[ADD_NAME] as String, allowsMultiple = false)
                }))
                executesPlayer(PlayerCommandExecutor { sender, args ->
                    NamespaceActions.add(sender, args.argsMap[ADD_NAME] as String)
                })
            }
        ))
        then(la("remove").then(
            StringArgument(REMOVE_NAME)
                .suggestNamespaceNames()
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    NamespaceActions.remove(sender, args.argsMap[REMOVE_NAME] as String)
                })
        ))
        then(la("rename").thenNested(
            StringArgument(RENAME_OLD).suggestNamespaceNames(),
            StringArgument(RENAME_NEW).executesPlayer(PlayerCommandExecutor { sender, args ->
                NamespaceActions.rename(sender, args.argsMap[RENAME_OLD] as String, args.argsMap[RENAME_NEW] as String)
            })
        ))
        then(la("info").then(
            StringArgument(INFO_NAME)
                .suggestNamespaceNames()
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    NamespaceActions.info(sender, args.argsMap[INFO_NAME] as String)
                })
        ))
    }
