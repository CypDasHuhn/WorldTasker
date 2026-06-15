package dev.cypdashuhn.worldtasker.commands.query

import dev.cypdashuhn.worldtasker.commands.suggestTagNamesDsl
import dev.cypdashuhn.worldtasker.commands.suggestTodoAuthors
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

/**
 * Each flag for the /todo get query. [buildInner] returns a fresh argument chain
 * for this flag (no LiteralArgument wrapper) with branches for [remaining] appended
 * plus a terminal executor.
 */
interface QueryFlag {
    val flagLiteral: String

    fun buildInner(remaining: List<Argument<*>>, executor: PlayerCommandExecutor): Argument<*>
}

/** Simple flag: one value argument. */
class SimpleQueryFlag(
    override val flagLiteral: String,
    private val valueArgFactory: () -> Argument<*>,
) : QueryFlag {
    override fun buildInner(remaining: List<Argument<*>>, executor: PlayerCommandExecutor): Argument<*> {
        val arg = valueArgFactory()
        remaining.forEach { arg.then(it) }
        arg.executesPlayer(executor)
        return arg
    }
}

/** Time flag: chains three arguments (type, operator, date). */
object TimeQueryFlag : QueryFlag {
    override val flagLiteral = "--time"

    override fun buildInner(remaining: List<Argument<*>>, executor: PlayerCommandExecutor): Argument<*> {
        val opArg = TextArgument(ARG_TIME_OP)
        val dateArg = TextArgument(ARG_TIME_DATE)
        remaining.forEach { dateArg.then(it) }
        dateArg.executesPlayer(executor)
        opArg.then(dateArg)

        val typeArg = TextArgument(ARG_TIME_TYPE)
        typeArg.then(opArg)
        return typeArg
    }
}

/** Boolean flag: no value argument. The literal itself is the node. */
class BooleanQueryFlag(
    override val flagLiteral: String,
) : QueryFlag {
    override fun buildInner(remaining: List<Argument<*>>, executor: PlayerCommandExecutor): Argument<*> {
        throw UnsupportedOperationException("BooleanQueryFlag handled directly in subtrees")
    }
}

internal const val ARG_NEAR_RADIUS = "nearRadius"
internal const val ARG_TAGS = "tags"
internal const val ARG_NAME = "name"
internal const val ARG_AUTHOR = "author"
internal const val ARG_TIME_TYPE = "timeType"
internal const val ARG_TIME_OP = "timeOperator"
internal const val ARG_TIME_DATE = "timeDate"

/** Full recursive builder for the /todo get query branches. */
object QueryTreeBuilder {
    private val flags: List<QueryFlag> = listOf(
        SimpleQueryFlag("--near") { IntegerArgument(ARG_NEAR_RADIUS) },
        SimpleQueryFlag("--tags") { TextArgument(ARG_TAGS).suggestTagNamesDsl() },
        SimpleQueryFlag("--name") { TextArgument(ARG_NAME) },
        SimpleQueryFlag("--author") { TextArgument(ARG_AUTHOR).suggestTodoAuthors() },
        TimeQueryFlag,
        BooleanQueryFlag("--completed"),
        BooleanQueryFlag("--random"),
    )

    /**
     * Returns all branches that can follow `get`.
     * Each branch is a [LiteralArgument("--flag")] wrapping the flag's inner chain.
     */
    fun build(executor: PlayerCommandExecutor): List<Argument<*>> {
        val memo = mutableMapOf<Set<QueryFlag>, List<Argument<*>>>()

        fun subtrees(available: Set<QueryFlag>): List<Argument<*>> =
            memo.getOrPut(available) {
                val branches = mutableListOf<Argument<*>>()
                for (flag in available) {
                    val remaining = available - flag
                    val childBranches = subtrees(remaining)
                    if (flag is BooleanQueryFlag) {
                        val lit = LiteralArgument(flag.flagLiteral)
                        childBranches.forEach { lit.then(it) }
                        lit.executesPlayer(executor)
                        branches.add(lit)
                    } else {
                        val inner = flag.buildInner(childBranches, executor)
                        branches.add(LiteralArgument(flag.flagLiteral).then(inner))
                    }
                }
                branches
            }
        return subtrees(flags.toSet())
    }
}
