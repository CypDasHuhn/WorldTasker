package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.query.ARG_AUTHOR
import dev.cypdashuhn.worldtasker.commands.query.ARG_NAME
import dev.cypdashuhn.worldtasker.commands.query.ARG_NEAR_RADIUS
import dev.cypdashuhn.worldtasker.commands.query.ARG_TAGS
import dev.cypdashuhn.worldtasker.commands.query.ARG_TIME_DATE
import dev.cypdashuhn.worldtasker.commands.query.ARG_TIME_OP
import dev.cypdashuhn.worldtasker.commands.query.ARG_TIME_TYPE
import dev.cypdashuhn.worldtasker.commands.query.QueryTreeBuilder
import dev.cypdashuhn.worldtasker.commands.query.TimeFilter
import dev.cypdashuhn.worldtasker.commands.query.TimeOperator
import dev.cypdashuhn.worldtasker.commands.query.TimeType
import dev.cypdashuhn.worldtasker.commands.query.TodoQuery
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import java.time.LocalDate

internal fun buildGetNode(): LiteralArgument {
    fun makeExecutor(showCompleted: Boolean, random: Boolean = false) = PlayerCommandExecutor { sender, args ->
        val query = TodoQuery(
            nearRadius = args.argsMap[ARG_NEAR_RADIUS] as? Int,
            tags = args.argsMap[ARG_TAGS] as? String,
            name = args.argsMap[ARG_NAME] as? String,
            author = args.argsMap[ARG_AUTHOR] as? String,
            showCompleted = showCompleted,
            timeFilter = (args.argsMap[ARG_TIME_TYPE] as? String)?.let {
                TimeFilter(
                    type = TimeType.valueOf(it.uppercase()),
                    operator = TimeOperator.valueOf((args.argsMap[ARG_TIME_OP] as String).uppercase()),
                    date = LocalDate.parse(args.argsMap[ARG_TIME_DATE] as String)
                )
            }
        )
        TodoActions.get(sender, query, random)
    }

    val executor = makeExecutor(false)
    val completedExecutor = makeExecutor(true)
    val randomExecutor = makeExecutor(false, random = true)

    val searchNode = LiteralArgument("search")
    searchNode.executesPlayer(executor)
    QueryTreeBuilder.build(executor).forEach { searchNode.then(it) }

    val completedNode = LiteralArgument("--completed")
    completedNode.executesPlayer(completedExecutor)
    QueryTreeBuilder.build(completedExecutor).forEach { completedNode.then(it) }
    searchNode.then(completedNode)

    val randomNode = LiteralArgument("--random")
    randomNode.executesPlayer(randomExecutor)
    QueryTreeBuilder.build(randomExecutor).forEach { randomNode.then(it) }
    searchNode.then(randomNode)

    return searchNode
}
