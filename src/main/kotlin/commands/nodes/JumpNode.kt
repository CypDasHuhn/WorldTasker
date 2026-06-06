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

private const val NAME = "jumpTodoName"

private fun buildJumpNameArg(filter: TodoNameFilter): Argument<NamespacedKey> {
    val nameArg = NamespacedKeyArgument(NAME).suggestScopedTodoNames(filter)
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, name ->
            TodoActions.jump(sender, name, id)
        }
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
