package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.commands.query.QueryTreeBuilder
import dev.cypdashuhn.worldtasker.commands.query.TimeFilter
import dev.cypdashuhn.worldtasker.commands.query.TimeOperator
import dev.cypdashuhn.worldtasker.commands.query.TimeType
import dev.cypdashuhn.worldtasker.commands.query.TodoQuery
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.entity.Player
import java.time.LocalDate

fun todo() {
    val executor = PlayerCommandExecutor { sender, args ->
        val query = TodoQuery(
            nearRadius = args.argsMap["nearRadius"] as? Int,
            tags = args.argsMap["tags"] as? String,
            name = args.argsMap["name"] as? String,
            author = args.argsMap["author"] as? String,
            timeFilter = (args.argsMap["timeType"] as? String)?.let {
                TimeFilter(
                    type = TimeType.valueOf(it.uppercase()),
                    operator = TimeOperator.valueOf((args.argsMap["timeOperator"] as String).uppercase()),
                    date = LocalDate.parse(args.argsMap["timeDate"] as String)
                )
            }
        )
        executeGetQuery(sender, query)
    }

    val getNode = LiteralArgument("get")
        .executesPlayer(executor) // no flags: get all

    // Attach all dynamically-generated flag branches
    QueryTreeBuilder.build(executor).forEach { branch ->
        getNode.then(branch)
    }

    CommandTree("todo")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.sendMessage("TODO! Use /todo get, /todo add, etc.")
        })
        .then(getNode)
        .register()
}

private fun executeGetQuery(sender: Player, query: TodoQuery) {
    val parts = mutableListOf<String>()
    parts.add("§6=== Todo Query Results ===")

    query.nearRadius?.let { parts.add("§e--near: §f$it chunks") }
    query.tags?.let { parts.add("§e--tags: §f$it") }
    query.name?.let { parts.add("§e--name: §f$it") }
    query.author?.let { parts.add("§e--author: §f$it") }
    query.timeFilter?.let { parts.add("§e--time: §f${it.type} ${it.operator} ${it.date}") }

    if (parts.size == 1) {
        parts.add("§7(no filters — all todos)")
    }

    parts.forEach { sender.sendMessage(it) }
}
