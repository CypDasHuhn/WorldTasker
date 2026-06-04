package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithTodo
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME = "infoTodoName"

private fun buildInfoNameArg(filter: TodoNameFilter): Argument<String> {
    val nameArg = StringArgument(NAME).suggestTodoNames(filter)
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NAME] as String
        handleWithTodo(sender, name, filter) { id -> TodoActions.info(sender, name, id) }
    })
    return nameArg
}

internal fun buildInfoNode(): LiteralArgument {
    val node = LiteralArgument("info")
    node.then(buildInfoNameArg(TodoNameFilter.ACTIVE))
    node.then(LiteralArgument("--completed").then(buildInfoNameArg(TodoNameFilter.COMPLETED)))
    node.then(LiteralArgument("--all").then(buildInfoNameArg(TodoNameFilter.ALL)))
    return node
}
