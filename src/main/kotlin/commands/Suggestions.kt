package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.CommandArguments
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal fun LiteralArgument.suggestedWhen(condition: (CommandArguments) -> Boolean): Argument<String> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        if (condition(info.previousArgs())) arrayOf(nodeName) else arrayOf()
    })

internal fun <T> Argument<T>.suggestScopedTodoNames(filter: TodoNameFilter = TodoNameFilter.ACTIVE): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        transaction {
            val allTodos = TodoManager.Todos.selectAll().mapNotNull { row ->
                val id = row[TodoManager.Todos.id].value
                if (filter.allowsState(TodoManager.stateOf(id))) Pair(id, row[TodoManager.Todos.name]) else null
            }
            allTodos.flatMap { (id, name) ->
                val scoped = if (TodoScopeManager.isActive())
                    TodoScopeManager.scopeTagNameForTodo(id)?.let { tag -> "$tag:$name" }
                else null
                listOfNotNull(name, scoped)
            }.distinct().toTypedArray()
        }
    })

internal fun <T> Argument<T>.suggestTodoNames(filter: TodoNameFilter = TodoNameFilter.ACTIVE): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { _ ->
        transaction {
            TodoManager.Todos.selectAll()
                .mapNotNull { row ->
                    val id = row[TodoManager.Todos.id].value
                    val state = TodoManager.stateOf(id)
                    if (filter.allowsState(state)) row[TodoManager.Todos.name] else null
                }
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
            (TagManager.Tags innerJoin NamespaceManager.Namespaces)
                .selectAll()
                .map { "${it[NamespaceManager.Namespaces.name]}:${it[TagManager.Tags.name]}" }
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
            (TagManager.Tags innerJoin NamespaceManager.Namespaces)
                .selectAll()
                .map { "${it[NamespaceManager.Namespaces.name]}:${it[TagManager.Tags.name]}" }
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

internal fun <T> Argument<T>.suggestTagNamesGreedy(): Argument<T> =
    replaceSuggestions(ArgumentSuggestions.strings { info ->
        val raw = info.currentArg()
        val parts = raw.split(" ")
        val done = if (raw.isEmpty() || raw.endsWith(" "))
            parts.filter { it.isNotEmpty() }.toSet()
        else
            parts.dropLast(1).filter { it.isNotEmpty() }.toSet()
        val prefix = if (done.isEmpty()) "" else done.joinToString(" ") + " "

        transaction {
            (TagManager.Tags innerJoin NamespaceManager.Namespaces)
                .selectAll()
                .map { "${it[NamespaceManager.Namespaces.name]}:${it[TagManager.Tags.name]}" }
                .filter { it !in done }
                .map { "$prefix$it" }
                .toTypedArray()
        }
    })
