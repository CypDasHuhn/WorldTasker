package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithScopedTodo
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.suggestScopedTodoNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesGreedy
import dev.cypdashuhn.worldtasker.commands.withFilters
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoResolveResult
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

private const val NAME = "editTodoName"
private const val STATE_ACTION = "editTodoStateAction"
private const val NEW_DESC = "editTodoNewDescription"
private const val SET_TAGS = "editTodoSetTags"
private const val ADD_TAGS = "editTodoAddTags"
private const val REMOVE_TAGS = "editTodoRemoveTags"
private const val WORK_COMMENT = "editTodoWorkComment"

data class EditCtx(
    val sender: Player,
    val id: Int,
    val name: String,
    val args: CommandArguments
)

private fun <T> Argument<T>.handle(filter: TodoNameFilter, block: EditCtx.() -> Unit): Argument<T> =
    executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, name ->
            block(EditCtx(sender, id, name, args))
        }
    })

private fun stateActionsFor(raw: Any?): Array<String> {
    val key: NamespacedKey = when (raw) {
        is NamespacedKey -> raw

        is String -> runCatching {
            if (':' in raw) {
                NamespacedKey(raw.substringBefore(':'), raw.substringAfter(':'))
            } else {
                NamespacedKey.minecraft(raw)
            }
        }.getOrNull()

        else -> null
    } ?: return arrayOf("complete", "reactivate")
    val result = TodoScopeManager.resolveInput(key, setOf(TodoState.ACTIVE, TodoState.COMPLETED))
    if (result !is TodoResolveResult.Found) return arrayOf("complete", "reactivate")
    return when (TodoManager.stateOf(result.id)) {
        TodoState.ACTIVE -> arrayOf("complete")
        TodoState.COMPLETED -> arrayOf("reactivate")
        else -> arrayOf()
    }
}

private fun buildEditNameArg(filter: TodoNameFilter): Argument<NamespacedKey> =
    NamespacedKeyArgument(NAME)
        .suggestScopedTodoNames(filter)
        .then(StringArgument(STATE_ACTION)
            .replaceSuggestions(ArgumentSuggestions.strings { info ->
                stateActionsFor(info.previousArgs().argsMap[NAME])
            })
            .handle(filter) {
                when (args.argsMap[STATE_ACTION] as String) {
                    "complete" -> TodoActions.complete(sender, id)
                    "reactivate" -> TodoActions.reactivate(sender, id)
                }
            }
        ).then(la("delete").handle(filter) {
            TodoActions.delete(sender, name, id)
        })
        .then(TextArgument(NEW_DESC).handle(filter) {
            TodoActions.updateDescription(sender, id, args.argsMap[NEW_DESC] as String)
        })
        .then(
            la("tags")
                .then(la("set").then(GreedyStringArgument(SET_TAGS).suggestTagNamesGreedy().handle(filter) {
                    TodoActions.setTags(sender, id, args.argsMap[SET_TAGS] as String)
                }))
                .then(la("add").then(GreedyStringArgument(ADD_TAGS).suggestTagNamesGreedy().handle(filter) {
                    TodoActions.addTags(sender, id, args.argsMap[ADD_TAGS] as String)
                }))
                .then(la("remove").then(GreedyStringArgument(REMOVE_TAGS).suggestTagNamesGreedy().handle(filter) {
                    TodoActions.removeTags(sender, id, args.argsMap[REMOVE_TAGS] as String)
                }))
        ).then(
            la("work").then(TextArgument(WORK_COMMENT).handle(filter) {
                TodoActions.work(sender, id, args.argsMap[WORK_COMMENT] as String)
            })
        )

internal fun buildEditNode() = la("edit").withFilters(::buildEditNameArg)
