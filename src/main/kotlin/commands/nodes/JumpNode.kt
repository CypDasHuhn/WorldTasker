package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithTodo
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME = "jumpTodoName"

private fun buildJumpNameArg(filter: TodoNameFilter): Argument<String> {
    val nameArg = StringArgument(NAME).suggestTodoNames(filter)
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NAME] as String
        handleWithTodo(sender, name, filter) { id -> TodoActions.jump(sender, name, id) }
    })
    return nameArg
}

internal fun buildJumpNode(): LiteralArgument {
    val node = LiteralArgument("jump")
    node.then(buildJumpNameArg(TodoNameFilter.ACTIVE))
    node.then(LiteralArgument("--completed").then(buildJumpNameArg(TodoNameFilter.COMPLETED)))
    node.then(LiteralArgument("--all").then(buildJumpNameArg(TodoNameFilter.ALL)))
    node.then(
        LiteralArgument("--random").executesPlayer(PlayerCommandExecutor { sender, _ ->
            TodoActions.jumpRandom(sender)
        })
    )
    return node
}
