package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.jetbrains.exposed.sql.transactions.transaction

internal fun buildJumpNode(): LiteralArgument {
    val nameArg = StringArgument("jumpTodoName").suggestTodoNames()
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap["jumpTodoName"] as String
        val todo = TodoManager.findByName(name)
        if (todo == null) {
            sender.msg("<red>Todo '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }
        val locId = todo[TodoManager.Todos.locationId]
        if (locId == null) {
            sender.msg("<red>Todo '<white>$name</white>' has no location.")
            return@PlayerCommandExecutor
        }
        val location = transaction {
            LocationManager.Location.findById(locId.value)?.location()
        }
        if (location == null) {
            sender.msg("<red>Location data for '<white>$name</white>' is missing.")
            return@PlayerCommandExecutor
        }
        sender.teleport(location)
        sender.msg("<green>Teleported to todo '<white>$name</white>'.")
    })
    val node = LiteralArgument("jump")
    node.then(nameArg)
    return node
}
