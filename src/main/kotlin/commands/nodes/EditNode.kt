package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.TodoActions
import dev.cypdashuhn.worldtasker.commands.TodoNameFilter
import dev.cypdashuhn.worldtasker.commands.handleWithTodo
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesCommaText
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME         = "editTodoName"
private const val NEW_DESC     = "editTodoNewDescription"
private const val SET_TAGS     = "editTodoSetTags"
private const val ADD_TAGS     = "editTodoAddTags"
private const val REMOVE_TAGS  = "editTodoRemoveTags"
private const val WORK_COMMENT = "editTodoWorkComment"

private fun buildEditNameArg(filter: TodoNameFilter): Argument<String> {
    val editNameArg = StringArgument(NAME).suggestTodoNames(filter)

    editNameArg.then(
        LiteralArgument("complete").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id -> TodoActions.complete(sender, id) }
        })
    )

    editNameArg.then(
        LiteralArgument("reactivate").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id -> TodoActions.reactivate(sender, id) }
        })
    )

    val newDescArg = TextArgument(NEW_DESC)
    newDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id ->
            TodoActions.updateDescription(sender, id, args.argsMap[NEW_DESC] as String)
        }
    })
    editNameArg.then(LiteralArgument("description").then(newDescArg))

    val editTagsNode = LiteralArgument("tags")

    val setTagsArg = TextArgument(SET_TAGS).suggestTagNamesCommaText()
    setTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id ->
            TodoActions.setTags(sender, id, args.argsMap[SET_TAGS] as String)
        }
    })
    editTagsNode.then(LiteralArgument("set").then(setTagsArg))

    val addTagsArg = TextArgument(ADD_TAGS).suggestTagNamesCommaText()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id ->
            TodoActions.addTags(sender, id, args.argsMap[ADD_TAGS] as String)
        }
    })
    editTagsNode.then(LiteralArgument("add").then(addTagsArg))

    val removeTagsArg = TextArgument(REMOVE_TAGS).suggestTagNamesCommaText()
    removeTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id ->
            TodoActions.removeTags(sender, id, args.argsMap[REMOVE_TAGS] as String)
        }
    })
    editTagsNode.then(LiteralArgument("remove").then(removeTagsArg))

    editNameArg.then(editTagsNode)

    val workCommentArg = TextArgument(WORK_COMMENT)
    workCommentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String, filter) { id ->
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
