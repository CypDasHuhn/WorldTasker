package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithScopedTodo
import dev.cypdashuhn.worldtasker.commands.suggestScopedTodoNames
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey

private const val NAME = "removeTodoName"

private fun buildRemoveNameArg(filter: TodoNameFilter): Argument<NamespacedKey> {
    val removeNameArg = NamespacedKeyArgument(NAME).suggestScopedTodoNames(filter)
    removeNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, name ->
            TodoActions.delete(sender, name, id)
        }
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
