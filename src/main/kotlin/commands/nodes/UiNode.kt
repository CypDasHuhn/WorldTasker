package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

internal fun buildUiNode(): LiteralArgument {
    val node = LiteralArgument("ui")
    node.executesPlayer(PlayerCommandExecutor { sender, _ ->
        TodoListInterface.openInventory(sender)
    })
    return node
}
