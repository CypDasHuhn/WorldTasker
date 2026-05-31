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
 * For TextArgument DSL tag filter: completes the tag name currently being typed while
 * preserving the already-typed operators and tag names as a prefix.
 *
 * e.g. if the user has typed "urgent+b the suggestions are "urgent+bug", "urgent+backend", …
 */
internal fun <T> Argument<T>.suggestTagNamesDsl(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        val raw = info.currentArg().removePrefix("\"")
        if (raw.trimEnd().endsWith(")")) return@strings emptyArray()
        val lastOpIdx = raw.indexOfLast { it in "+,-(" }
        val prefix = if (lastOpIdx == -1) "" else raw.substring(0, lastOpIdx + 1)
        val currentToken = if (lastOpIdx == -1) raw else raw.substring(lastOpIdx + 1)
        transaction {
            TagManager.all()
                .map { it[TagManager.Tags.name] }
                .filter { it.startsWith(currentToken, ignoreCase = true) }
                .map { "\"$prefix$it\"" }
                .toTypedArray()
        }
    })

internal fun <T> Argument<T>.suggestTodoAuthors(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        transaction {
            TodoManager.Todos.selectAll()
                .map { it[TodoManager.Todos.author] }
                .distinct()
                .toTypedArray()
        }
    })

/**
 * For TextArgument comma-separated tag lists: suggestions are full quoted strings including
 * already-typed tags as a prefix, so selecting appends rather than replaces.
 *
 * e.g. if the user has typed "urgent, the suggestions are "urgent,bug", "urgent,docs", …
 */
internal fun <T> Argument<T>.suggestTagNamesCommaText(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        val raw = info.currentArg().removePrefix("\"")
        val parts = raw.split(",")
        val done = if (raw.isEmpty() || raw.endsWith(","))
            parts.filter { it.isNotEmpty() }.toSet()
        else
            parts.dropLast(1).filter { it.isNotEmpty() }.toSet()
        val prefix = if (done.isEmpty()) "" else done.joinToString(",") + ","

        transaction {
            TagManager.all()
                .map { it[TagManager.Tags.name] }
                .filter { it !in done }
                .map { "\"$prefix$it\"" }
                .toTypedArray()
        }
    })
