package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.cypdashuhn.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.jetbrains.exposed.sql.transactions.transaction

const val JUMP_NODE_NAME = "jumpTodoName"

internal fun buildJumpNode(): LiteralArgument {
    val nameArg = StringArgument(JUMP_NODE_NAME).suggestTodoNames()
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[JUMP_NODE_NAME] as String
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
            LocationManager.Location.findById(locId)?.location()
        }
        if (location == null) {
            sender.msg("<red>Location data for '<white>$name</white>' is missing.")
            return@PlayerCommandExecutor
        }
        sender.teleport(location)
        sender.msg("<green>Teleported to todo '<white>$name</white>'.")

        val id = todo[TodoManager.Todos.id].value
        val description = todo[TodoManager.Todos.description]
        val tags = TagManager.tagLabelsForTodo(id)
        if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
        sender.msg("<gray>\"$description\"")
    })
    val node = LiteralArgument("jump")
    node.then(nameArg)
    return node
}
