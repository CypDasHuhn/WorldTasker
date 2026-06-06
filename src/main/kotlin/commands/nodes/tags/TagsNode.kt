package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.actions.TagActions
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey

private const val ADD_NS = "todoTagsAddNs"
private const val ADD_NAME = "todoTagsAddName"
private const val REMOVE_NAME = "todoTagsRemoveName"
private const val RENAME_OLD = "todoTagsRenameOld"
private const val RENAME_NEW = "todoTagsRenameNew"

internal fun buildTodoTagsNode(): LiteralArgument =
    la("tags").apply {
        then(la("list").executesPlayer(PlayerCommandExecutor { sender, _ -> TagActions.list(sender) }))
        then(la("add").thenNested(
            StringArgument(ADD_NS).suggestNamespaceNames(),
            StringArgument(ADD_NAME).executesPlayer(PlayerCommandExecutor { sender, args ->
                TagActions.add(sender, args.argsMap[ADD_NS] as String, args.argsMap[ADD_NAME] as String)
            })
        ))
        then(la("remove").then(
            NamespacedKeyArgument(REMOVE_NAME)
                .suggestTagNames()
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    TagActions.remove(sender, args.argsMap[REMOVE_NAME] as NamespacedKey)
                })
        ))
        then(la("rename").thenNested(
            NamespacedKeyArgument(RENAME_OLD).suggestTagNames(),
            StringArgument(RENAME_NEW).executesPlayer(PlayerCommandExecutor { sender, args ->
                TagActions.rename(sender, args.argsMap[RENAME_OLD] as NamespacedKey, args.argsMap[RENAME_NEW] as String)
            })
        ))
        then(buildInheritNode())
        then(buildNamespacesNode())
    }
