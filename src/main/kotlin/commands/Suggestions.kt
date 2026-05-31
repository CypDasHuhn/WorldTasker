package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal fun <T> Argument<T>.suggestTodoNames(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        transaction {
            TodoManager.Todos.selectAll()
                .map { it[TodoManager.Todos.name] }
                .toTypedArray()
        }
    })

internal fun <T> Argument<T>.suggestNamespaceNames(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        transaction {
            NamespaceManager.all()
                .map { it[NamespaceManager.Namespaces.name] }
                .toTypedArray()
        }
    })

internal fun <T> Argument<T>.suggestTagNames(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        transaction {
            TagManager.all()
                .map { it[TagManager.Tags.name] }
                .toTypedArray()
        }
    })

/**
 * For GreedyStringArgument tag lists: each suggestion includes the already-confirmed tags as a
 * prefix so that selecting a completion appends rather than replaces.
 *
 * e.g. if the user has typed "urgent " the suggestions are "urgent bug", "urgent docs", …
 */
internal fun <T> Argument<T>.suggestTagNamesGreedy(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        val current = info.currentArg()
        val parts = current.split(Regex("[,\\s]+")).filter { it.isNotEmpty() }
        // Tags already fully typed (everything except the word currently being typed)
        val done = if (current.isEmpty() || current.last() == ' ' || current.last() == ',')
            parts.toSet()
        else
            parts.dropLast(1).toSet()
        val prefix = if (done.isEmpty()) "" else done.joinToString(" ") + " "

        transaction {
            TagManager.all()
                .map { it[TagManager.Tags.name] }
                .filter { it !in done }
                .map { "$prefix$it" }
                .toTypedArray()
        }
    })
