package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithTodo
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME = "removeTodoName"

private fun buildRemoveNameArg(filter: TodoNameFilter): Argument<String> {
    val removeNameArg = StringArgument(NAME).suggestTodoNames(filter)
    removeNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NAME] as String
        handleWithTodo(sender, name, filter) { id -> TodoActions.delete(sender, name, id) }
    })
    return removeNameArg
}

internal fun buildRemoveNode(): LiteralArgument {
    val node = LiteralArgument("remove")
    node.then(buildRemoveNameArg(TodoNameFilter.ACTIVE))
    node.then(LiteralArgument("--completed").then(buildRemoveNameArg(TodoNameFilter.COMPLETED)))
    node.then(LiteralArgument("--all").then(buildRemoveNameArg(TodoNameFilter.ALL)))
    return node
}
