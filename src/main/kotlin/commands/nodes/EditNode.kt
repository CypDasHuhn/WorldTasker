package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithScopedTodo
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.suggestScopedTodoNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesGreedy
import dev.cypdashuhn.worldtasker.commands.suggestedWhen
import dev.cypdashuhn.worldtasker.commands.withFilters
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoResolveResult
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

private const val NAME = "editTodoName"
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

private fun todoInState(vararg states: TodoState): (CommandArguments) -> Boolean =
    { prevArgs ->
        val key = prevArgs.argsMap[NAME] as? NamespacedKey
        if (key == null) {
            true
        } else {
            val result = TodoScopeManager.resolveInput(key, TodoState.values().toSet() - setOf(TodoState.DELETED))
            result is TodoResolveResult.Found && TodoManager.stateOf(result.id) in states
        }
    }

private fun buildEditNameArg(filter: TodoNameFilter): Argument<NamespacedKey> =
    NamespacedKeyArgument(NAME)
        .suggestScopedTodoNames(filter)
        .then(la("complete").suggestedWhen(todoInState(TodoState.ACTIVE)).handle(filter) {
            TodoActions.complete(sender, id)
        })
        .then(la("reactivate").suggestedWhen(todoInState(TodoState.COMPLETED)).handle(filter) {
            TodoActions.reactivate(sender, id)
        })
        .then(la("delete").handle(filter) {
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
