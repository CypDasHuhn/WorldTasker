package dev.cypdashuhn.worldtasker.commands.wrapper

import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.executors.CommandArguments
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.lang.reflect.Field

fun error(value: String, appendInput: Boolean = false): CustomArgument.CustomArgumentException {
    return CustomArgument.CustomArgumentException.fromMessageBuilder(
        CustomArgument.MessageBuilder(value).run {
            if (appendInput) appendArgInput() else this
        }
    )
}

fun <T> Argument<T>.simpleSuggestions(vararg suggestion: String): Argument<T> {
    return replaceSuggestions(ArgumentSuggestions.strings { suggestion })
}

fun existsByList(list: List<String>): ((String) -> Boolean) = { list.contains(it) }
fun existsByColumn(column: Column<String>): ((String) -> Boolean) = { column.valueByColumn(it) != null }

fun <T> Column<T>.valueByColumn(value: T): ResultRow? {
    val table = this.table
    return table.selectAll().where(this eq value).firstOrNull()
}

fun findFieldRecursive(clazz: Class<*>, name: String): Field {
    var current: Class<*>? = clazz
    while (current != null) {
        try {
            return current.getDeclaredField(name)
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    throw NoSuchFieldException(name)
}


inline fun <reified T> CommandArguments.eitherOf(vararg nodeNames: String): T {
    argsMap.filter { it.key in nodeNames }.forEach {
        if (it.value is T) return it.value as T
    }
    throw error("No value found")
}

inline fun <reified T> CommandArguments.safeEitherOf(vararg nodeNames: String): T? {
    argsMap.filter { it.key in nodeNames }.forEach {
        if (it.value is T) return it.value as T
    }
    return null
}

fun List<Int>.padded(): List<String> {
    val max = this.maxOrNull() ?: 0
    return map { it.toString().padStart(max.toString().length, '0') }
}

fun <T, B> Argument<T>.transform(parser: CustomArgument.CustomArgumentInfoParser<B, T>): CustomArgument<B, T> {
    return CustomArgument<B, T>(this, parser)
}
