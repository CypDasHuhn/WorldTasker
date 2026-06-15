package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.msg
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val TOPIC = "topic"

private val topics = mapOf(
    "filters" to {
        listOf(
            "<gold>=== Filters ===",
            "<gray>Filters let you narrow down the todo list by tags, status, authors, or distance.",
            "<gray>Open the filter interface with <white>/todo filters</white> or from the <white>Filters</white> button in the todo list.",
        ).joinToString("<br>")
    },
    "tags" to {
        listOf(
            "<gold>=== Tags ===",
            "<gray>Tags are grouped into namespaces. Each namespace can hold multiple tags.",
            "<gray>Manage namespaces and tags with <white>/todo namespaces</white>.",
            "<gray>Assign tags to todos from the todo detail view or via the namespaces UI.",
        ).joinToString("<br>")
    },
)

private val overview = listOf(
    "<gold>=== Todo Help ===",
    "<gray>Welcome to Todo! Manage your tasks with a powerful tagging and filtering system.",
    "<gray>Commands:",
    "<gray>  <white>/todo</white> — Open the main todo list",
    "<gray>  <white>/todo help</white> — Show this help",
    "<gray>  <white>/todo help <topic></white> — Show help for a specific topic",
    "<gray>  <white>/todo get <name></white> — Open a todo's detail view",
    "<gray>  <white>/todo add <name></white> — Create a new todo",
    "<gray>  <white>/todo edit <name></white> — Edit a todo",
    "<gray>  <white>/todo remove <name></white> — Remove a todo",
    "<gray>  <white>/todo filters</white> — Open the filter interface",
    "<gray>  <white>/todo namespaces</white> — Manage namespaces and tags",
    "<gray>",
    "<gray>Available help topics: <white>${topics.keys.joinToString(", ")}</white>",
).joinToString("<br>")

internal fun buildHelpNode(): LiteralArgument =
    la("help").apply {
        executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.msg(overview)
        })
        then(
            StringArgument(TOPIC).executesPlayer(PlayerCommandExecutor { sender, args ->
                val topic = (args.argsMap[TOPIC] as String).lowercase()
                val helpText = topics[topic]
                    ?: "<red>Unknown topic '<white>$topic</white>'. Available: <white>${topics.keys.joinToString(", ")}</white>"
                sender.msg(helpText)
            })
        )
    }
