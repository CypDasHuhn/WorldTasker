package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.actions.TagActions
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey

private const val ADD_NS      = "todoTagsAddNs"
private const val ADD_NAME    = "todoTagsAddName"
private const val REMOVE_NAME = "todoTagsRemoveName"
private const val RENAME_OLD  = "todoTagsRenameOld"
private const val RENAME_NEW  = "todoTagsRenameNew"

internal fun buildTodoTagsNode(): LiteralArgument {
    val tagsNode = LiteralArgument("tags")

    tagsNode.then(
        LiteralArgument("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            TagActions.list(sender)
        })
    )

    val tagsAddNsArg = StringArgument(ADD_NS).suggestNamespaceNames()
    val tagsAddNameArg = StringArgument(ADD_NAME)
    tagsAddNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TagActions.add(sender, args.argsMap[ADD_NS] as String, args.argsMap[ADD_NAME] as String)
    })
    tagsAddNsArg.then(tagsAddNameArg)
    tagsNode.then(LiteralArgument("add").then(tagsAddNsArg))

    val tagsRemoveNameArg = NamespacedKeyArgument(REMOVE_NAME).suggestTagNames()
    tagsRemoveNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TagActions.remove(sender, args.argsMap[REMOVE_NAME] as NamespacedKey)
    })
    tagsNode.then(LiteralArgument("remove").then(tagsRemoveNameArg))

    val tagsRenameOldArg = NamespacedKeyArgument(RENAME_OLD).suggestTagNames()
    val tagsRenameNewArg = StringArgument(RENAME_NEW)
    tagsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TagActions.rename(sender, args.argsMap[RENAME_OLD] as NamespacedKey, args.argsMap[RENAME_NEW] as String)
    })
    tagsRenameOldArg.then(tagsRenameNewArg)
    tagsNode.then(LiteralArgument("rename").then(tagsRenameOldArg))

    tagsNode.then(buildInheritNode())
    tagsNode.then(buildNamespacesNode())

    return tagsNode
}
