package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesGreedy
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME = "addTodoName"
private const val DESCRIPTION = "addTodoDescription"
private const val TAGS = "addTodoTags"

fun buildAddNode() =
    LiteralArgument("add").thenNested(
        StringArgument(NAME),
        TextArgument(DESCRIPTION).executesPlayer(executor),
        GreedyStringArgument(TAGS).suggestTagNamesGreedy().executesPlayer(executor)
    )

val executor = PlayerCommandExecutor { sender, args ->
    TodoActions.add(
        sender,
        args.argsMap[NAME] as String,
        args.argsMap[DESCRIPTION] as String,
        args.argsMap[TAGS] as? String?
    )
}
