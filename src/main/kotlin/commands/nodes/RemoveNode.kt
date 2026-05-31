package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.handleWithTodo
import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME = "removeTodoName"

internal fun buildRemoveNode(): LiteralArgument {
    val removeNameArg = StringArgument(NAME).suggestTodoNames()
    removeNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NAME] as String
        handleWithTodo(sender, name) { id ->
            TodoManager.delete(id, sender.name)
            sender.msg("<green>Todo '<white>$name</white>' removed.")
        }
    })
    val node = LiteralArgument("remove")
    node.then(removeNameArg)
    return node
}
