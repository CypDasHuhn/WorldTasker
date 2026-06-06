package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesGreedy
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME        = "addTodoName"
private const val DESCRIPTION = "addTodoDescription"
private const val TAGS        = "addTodoTags"

internal fun buildAddNode(): LiteralArgument {
    val addTagsArg = GreedyStringArgument(TAGS).suggestTagNamesGreedy()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TodoActions.add(sender, args.argsMap[NAME] as String, args.argsMap[DESCRIPTION] as String, args.argsMap[TAGS] as String)
    })

    val addDescArg = TextArgument(DESCRIPTION)
    addDescArg.then(addTagsArg)
    addDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TodoActions.add(sender, args.argsMap[NAME] as String, args.argsMap[DESCRIPTION] as String, null)
    })

    val addNameArg = StringArgument(NAME)
    addNameArg.then(addDescArg)
    val node = LiteralArgument("add")
    node.then(addNameArg)
    return node
}
