package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithScopedTodo
import dev.cypdashuhn.worldtasker.commands.suggestScopedTodoNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesGreedy
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey

private const val NAME         = "editTodoName"
private const val NEW_DESC     = "editTodoNewDescription"
private const val SET_TAGS     = "editTodoSetTags"
private const val ADD_TAGS     = "editTodoAddTags"
private const val REMOVE_TAGS  = "editTodoRemoveTags"
private const val WORK_COMMENT = "editTodoWorkComment"

private fun buildEditNameArg(filter: TodoNameFilter): Argument<NamespacedKey> {
    val editNameArg = NamespacedKeyArgument(NAME).suggestScopedTodoNames(filter)

    editNameArg.then(
        LiteralArgument("complete").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
                TodoActions.complete(sender, id)
            }
        })
    )

    editNameArg.then(
        LiteralArgument("reactivate").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
                TodoActions.reactivate(sender, id)
            }
        })
    )

    val newDescArg = TextArgument(NEW_DESC)
    newDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
            TodoActions.updateDescription(sender, id, args.argsMap[NEW_DESC] as String)
        }
    })
    editNameArg.then(LiteralArgument("description").then(newDescArg))

    val editTagsNode = LiteralArgument("tags")

    val setTagsArg = GreedyStringArgument(SET_TAGS).suggestTagNamesGreedy()
    setTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
            TodoActions.setTags(sender, id, args.argsMap[SET_TAGS] as String)
        }
    })
    editTagsNode.then(LiteralArgument("set").then(setTagsArg))

    val addTagsArg = GreedyStringArgument(ADD_TAGS).suggestTagNamesGreedy()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
            TodoActions.addTags(sender, id, args.argsMap[ADD_TAGS] as String)
        }
    })
    editTagsNode.then(LiteralArgument("add").then(addTagsArg))

    val removeTagsArg = GreedyStringArgument(REMOVE_TAGS).suggestTagNamesGreedy()
    removeTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
            TodoActions.removeTags(sender, id, args.argsMap[REMOVE_TAGS] as String)
        }
    })
    editTagsNode.then(LiteralArgument("remove").then(removeTagsArg))

    editNameArg.then(editTagsNode)

    val workCommentArg = TextArgument(WORK_COMMENT)
    workCommentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithScopedTodo(sender, args.argsMap[NAME] as NamespacedKey, filter) { id, _ ->
            TodoActions.work(sender, id, args.argsMap[WORK_COMMENT] as String)
        }
    })
    editNameArg.then(LiteralArgument("work").then(workCommentArg))

    return editNameArg
}

internal fun buildEditNode(): LiteralArgument {
    val node = LiteralArgument("edit")
    node.then(buildEditNameArg(TodoNameFilter.ACTIVE))
    node.then(LiteralArgument("--completed").then(buildEditNameArg(TodoNameFilter.COMPLETED)))
    node.then(LiteralArgument("--all").then(buildEditNameArg(TodoNameFilter.ALL)))
    return node
}
