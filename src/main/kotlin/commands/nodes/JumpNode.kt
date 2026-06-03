package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.allowsState
import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

private const val NAME = "jumpTodoName"

private fun buildJumpNameArg(filter: TodoNameFilter): Argument<String> {
    val nameArg = StringArgument(NAME).suggestTodoNames(filter)
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NAME] as String
        val todo = TodoManager.findByName(name)
        if (todo == null) {
            sender.msg("<red>Todo '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }
        val id = todo[TodoManager.Todos.id].value
        if (!filter.allowsState(TodoManager.stateOf(id))) {
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

        val description = todo[TodoManager.Todos.description]
        val tags = TagManager.tagLabelsForTodo(id)
        if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
        sender.msg("<gray>\"$description\"")
    })
    return nameArg
}

private fun doJumpRandom(sender: org.bukkit.entity.Player) {
    val candidates = transaction { TodoManager.Todos.selectAll().toList() }
        .filter { row ->
            row[TodoManager.Todos.locationId] != null &&
                TodoManager.stateOf(row[TodoManager.Todos.id].value) == TodoState.ACTIVE
        }
    val todo = candidates.randomOrNull()
    if (todo == null) {
        sender.msg("<red>No active todos with a location found.")
        return
    }
    val id = todo[TodoManager.Todos.id].value
    val name = todo[TodoManager.Todos.name]
    val locId = todo[TodoManager.Todos.locationId]!!
    val location = transaction { LocationManager.Location.findById(locId)?.location() }
    if (location == null) {
        sender.msg("<red>Location data for '<white>$name</white>' is missing.")
        return
    }
    sender.teleport(location)
    sender.msg("<green>Teleported to random todo '<white>$name</white>'.")

    val description = todo[TodoManager.Todos.description]
    val tags = TagManager.tagLabelsForTodo(id)
    if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
    sender.msg("<gray>\"$description\"")
}

internal fun buildJumpNode(): LiteralArgument {
    val node = LiteralArgument("jump")
    node.then(buildJumpNameArg(TodoNameFilter.ACTIVE))
    node.then(LiteralArgument("--completed").then(buildJumpNameArg(TodoNameFilter.COMPLETED)))
    node.then(LiteralArgument("--all").then(buildJumpNameArg(TodoNameFilter.ALL)))
    node.then(
        LiteralArgument("--random").executesPlayer(PlayerCommandExecutor { sender, _ ->
            doJumpRandom(sender)
        })
    )
    return node
}
