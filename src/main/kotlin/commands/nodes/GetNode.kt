package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.query.QueryTreeBuilder
import dev.cypdashuhn.worldtasker.commands.query.TimeFilter
import dev.cypdashuhn.worldtasker.commands.query.TimeOperator
import dev.cypdashuhn.worldtasker.commands.query.TimeType
import dev.cypdashuhn.worldtasker.commands.query.TodoQuery
import dev.cypdashuhn.worldtasker.commands.query.executeTodoQuery
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.entity.Player
import java.time.LocalDate

internal fun buildGetNode(): LiteralArgument {
    fun makeExecutor(showCompleted: Boolean) = PlayerCommandExecutor { sender, args ->
        val query = TodoQuery(
            nearRadius = args.argsMap["nearRadius"] as? Int,
            tags = args.argsMap["tags"] as? String,
            name = args.argsMap["name"] as? String,
            author = args.argsMap["author"] as? String,
            showCompleted = showCompleted,
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

    val executor = makeExecutor(false)
    val completedExecutor = makeExecutor(true)

    val getNode = LiteralArgument("get")
    getNode.executesPlayer(executor)
    QueryTreeBuilder.build(executor).forEach { getNode.then(it) }

    val completedNode = LiteralArgument("--completed")
    completedNode.executesPlayer(completedExecutor)
    QueryTreeBuilder.build(completedExecutor).forEach { completedNode.then(it) }
    getNode.then(completedNode)

    return getNode
}

private fun executeGetQuery(sender: Player, query: TodoQuery) {
    val results = executeTodoQuery(query, sender.location)

    if (results.isEmpty()) {
        sender.msg("<gray>No todos found.")
        return
    }

    val cap = 10
    val shown = results.take(cap)
    val countLabel = if (results.size > cap) "<yellow>${results.size}</yellow> <gold>found, showing first $cap" else "<yellow>${results.size}</yellow> <gold>found"
    sender.msg("<gold>=== Todos $countLabel <gold>===")

    shown.forEach { row ->
        val id = row[TodoManager.Todos.id].value
        val name = row[TodoManager.Todos.name]
        val state = TodoManager.stateOf(id)
        val tags = TagManager.tagLabelsForTodo(id)

        val stateSuffix = if (state == TodoState.COMPLETED) " <green>[✓]" else ""
        val tagSuffix = if (tags.isNotEmpty()) " <dark_gray>| <gray>${tags.joinToString(", ")}" else ""
        sender.msg("<yellow>#$id <white>$name$stateSuffix$tagSuffix")
    }
}
