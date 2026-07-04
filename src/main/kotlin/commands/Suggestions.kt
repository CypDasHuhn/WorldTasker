package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.SuggestionCache
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.CommandArguments

internal fun LiteralArgument.suggestedWhen(condition: (CommandArguments) -> Boolean): Argument<String> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        if (condition(info.previousArgs())) arrayOf(nodeName) else arrayOf()
    })

internal fun <T> Argument<T>.suggestScopedTodoNames(filter: TodoNameFilter = TodoNameFilter.ACTIVE): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        SuggestionCache.getTodos()
            .filter { filter.allowsState(it.state) }
            .flatMap { todo ->
                val scopeTag = if (TodoScopeManager.isActive()) TodoScopeManager.scopeTagNameForTodo(todo.id) else null
                val scoped = scopeTag?.let { "$it:${todo.name}" }
                val noNs = if (TodoScopeManager.isActive() && scopeTag == null) "no-namespace:${todo.name}" else null
                listOfNotNull(todo.name, scoped, noNs)
            }.distinct()
            .toTypedArray()
    })

internal fun <T> Argument<T>.suggestTodoNames(filter: TodoNameFilter = TodoNameFilter.ACTIVE): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        SuggestionCache.getTodos()
            .filter { filter.allowsState(it.state) }
            .map { it.name }
            .toTypedArray()
    })

internal fun <T> Argument<T>.suggestNamespaceNames(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        SuggestionCache.getNamespaceNames().toTypedArray()
    })

internal fun <T> Argument<T>.suggestTagNames(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        SuggestionCache.getTagNames().toTypedArray()
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
        val lastOpIdx = raw.indexOfLast { it in "+,(" }
        val prefix = if (lastOpIdx == -1) "" else raw.substring(0, lastOpIdx + 1)
        val rawToken = if (lastOpIdx == -1) raw else raw.substring(lastOpIdx + 1)
        val notPrefix = if (rawToken.startsWith("-")) "-" else ""
        val currentToken = rawToken.removePrefix("-")
        SuggestionCache.getTagNames()
            .filter { it.startsWith(currentToken, ignoreCase = true) }
            .map { "\"$prefix$notPrefix$it\"" }
            .toTypedArray()
    })

internal fun <T> Argument<T>.suggestTodoAuthors(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        SuggestionCache.getAuthors().toTypedArray()
    })

internal fun <T> Argument<T>.suggestTimeType(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        arrayOf("created", "worked", "completed")
    })

internal fun <T> Argument<T>.suggestTimeOperator(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        arrayOf("before", "after", "on", "<", ">", "=")
    })

internal fun <T> Argument<T>.suggestTimeDate(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        arrayOf(java.time.LocalDate.now().toString())
    })

internal fun <T> Argument<T>.suggestTagNamesGreedy(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        val raw = info.currentArg()
        val parts = raw.split(" ")
        val done = if (raw.isEmpty() || raw.endsWith(" ")) {
            parts.filter { it.isNotEmpty() }.toSet()
        } else {
            parts.dropLast(1).filter { it.isNotEmpty() }.toSet()
        }
        val prefix = if (done.isEmpty()) "" else done.joinToString(" ") + " "

        SuggestionCache.getTagNames()
            .filter { it !in done }
            .map { "$prefix$it" }
            .toTypedArray()
    })
