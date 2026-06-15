package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.la
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
    fun makeExecutor() =
        PlayerCommandExecutor { sender, args ->
            val query = TodoQuery(
                nearRadius = args.argsMap[ARG_NEAR_RADIUS] as? Int,
                tags = args.argsMap[ARG_TAGS] as? String,
                name = args.argsMap[ARG_NAME] as? String,
                author = args.argsMap[ARG_AUTHOR] as? String,
                showCompleted = "--completed" in args.fullInput(),
                timeFilter = (args.argsMap[ARG_TIME_TYPE] as? String)?.let {
                    TimeFilter(
                        type = TimeType.valueOf(it.uppercase()),
                        operator = TimeOperator.valueOf((args.argsMap[ARG_TIME_OP] as String).uppercase()),
                        date = LocalDate.parse(args.argsMap[ARG_TIME_DATE] as String)
                    )
                }
            )
            TodoActions.get(sender, query, "--random" in args.fullInput())
        }

    return la("search").apply {
        val exec = makeExecutor()
        executesPlayer(exec)
        QueryTreeBuilder.build(exec).forEach { then(it) }
    }
}
