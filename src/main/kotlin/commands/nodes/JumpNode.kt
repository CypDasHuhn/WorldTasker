package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithScopedTodo
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.suggestScopedTodoNames
import dev.cypdashuhn.worldtasker.commands.withFilters
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey

private const val NAME = "jumpTodoName"

private fun jumpNameArg(filter: TodoNameFilter) =
    NamespacedKeyArgument(NAME).suggestScopedTodoNames(filter)
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, name ->
                TodoActions.jump(sender, name, id)
            }
        })

internal fun buildJumpNode() =
    la("jump").withFilters(::jumpNameArg).apply {
        then(la("--random").executesPlayer(PlayerCommandExecutor { sender, _ -> TodoActions.jumpRandom(sender) }))
    }
