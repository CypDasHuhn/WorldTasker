package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.msg
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
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.entity.Player
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal fun buildGetNode(): LiteralArgument {
    fun parseTimeFilter(sender: Player, args: CommandArguments): TimeFilter? {
        val typeStr = args.argsMap[ARG_TIME_TYPE] as? String ?: return null
        val type = try {
            TimeType.valueOf(typeStr.uppercase())
        } catch (_: IllegalArgumentException) {
            sender.msg("<red>Invalid time type '<white>$typeStr</white>'. Use: created, worked, or completed.")
            return null
        }
        val opStr = args.argsMap[ARG_TIME_OP] as String
        val operator = try {
            when (opStr) {
                "<" -> TimeOperator.BEFORE
                ">" -> TimeOperator.AFTER
                "=" -> TimeOperator.ON
                else -> TimeOperator.valueOf(opStr.uppercase())
            }
        } catch (_: IllegalArgumentException) {
            sender.msg("<red>Invalid time operator '<white>$opStr</white>'. Use: before, after, on, &lt;, &gt;, or =.")
            return null
        }
        val dateStr = args.argsMap[ARG_TIME_DATE] as String
        val date = try {
            LocalDate.parse(dateStr)
        } catch (_: DateTimeParseException) {
            sender.msg("<red>Invalid date '<white>$dateStr</white>'. Use the format yyyy-MM-dd (e.g. ${LocalDate.now()}).")
            return null
        }
        return TimeFilter(type, operator, date)
    }

    fun makeExecutor() =
        PlayerCommandExecutor { sender, args ->
            val timeFilter = parseTimeFilter(sender, args)
            if (timeFilter == null && args.argsMap.containsKey(ARG_TIME_TYPE)) return@PlayerCommandExecutor

            val query = TodoQuery(
                nearRadius = args.argsMap[ARG_NEAR_RADIUS] as? Int,
                tags = args.argsMap[ARG_TAGS] as? String,
                name = args.argsMap[ARG_NAME] as? String,
                author = args.argsMap[ARG_AUTHOR] as? String,
                showCompleted = "--completed" in args.fullInput(),
                timeFilter = timeFilter,
            )
            TodoActions.get(sender, query, "--random" in args.fullInput())
        }

    return la("search").apply {
        val exec = makeExecutor()
        executesPlayer(exec)
        QueryTreeBuilder.build(exec).forEach { then(it) }
    }
}
