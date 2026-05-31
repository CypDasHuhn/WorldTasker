package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

internal fun buildRemoveNode(): LiteralArgument {
    val removeNameArg = StringArgument("removeTodoName").suggestTodoNames()
    removeNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap["removeTodoName"] as String
        handleWithTodo(sender, name) { id ->
            TodoManager.delete(id, sender.name)
            sender.msg("<green>Todo '<white>$name</white>' removed.")
        }
    })
    val node = LiteralArgument("remove")
    node.then(removeNameArg)
    return node
}
