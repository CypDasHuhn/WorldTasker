package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.handleWithTodo
import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.resolveTagIds
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesCommaText
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoStatus
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

internal fun buildEditNode(): LiteralArgument {
    val editNameArg = StringArgument(NAME).suggestTodoNames()

    editNameArg.then(
        LiteralArgument("complete").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
                TodoManager.complete(id, sender.name)
                sender.msg("<green>Todo marked complete.")
            }
        })
    )

    editNameArg.then(
        LiteralArgument("reactivate").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
                TodoManager.reactivate(id, sender.name)
                sender.msg("<green>Todo reactivated.")
            }
        })
    )

    val newDescArg = TextArgument(NEW_DESC)
    newDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
            TodoManager.updateDescription(id, args.argsMap[NEW_DESC] as String)
            sender.msg("<green>Description updated.")
        }
    })
    editNameArg.then(LiteralArgument("description").then(newDescArg))

    val editTagsNode = LiteralArgument("tags")

    val setTagsArg = TextArgument(SET_TAGS).suggestTagNamesCommaText()
    setTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
            TagManager.removeAllForTodo(id)
            resolveTagIds(args.argsMap[SET_TAGS] as String, sender).forEach { TagManager.addToTodo(id, it) }
            sender.msg("<green>Tags set.")
        }
    })
    editTagsNode.then(LiteralArgument("set").then(setTagsArg))

    val addTagsArg = TextArgument(ADD_TAGS).suggestTagNamesCommaText()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
            resolveTagIds(args.argsMap[ADD_TAGS] as String, sender).forEach { TagManager.addToTodo(id, it) }
            sender.msg("<green>Tags added.")
        }
    })
    editTagsNode.then(LiteralArgument("add").then(addTagsArg))

    val removeTagsArg = TextArgument(REMOVE_TAGS).suggestTagNamesCommaText()
    removeTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
            resolveTagIds(args.argsMap[REMOVE_TAGS] as String, sender).forEach { TagManager.removeFromTodo(id, it) }
            sender.msg("<green>Tags removed.")
        }
    })
    editTagsNode.then(LiteralArgument("remove").then(removeTagsArg))

    editNameArg.then(editTagsNode)

    val workCommentArg = TextArgument(WORK_COMMENT)
    workCommentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap[NAME] as String) { id ->
            HistoryManager.record(id, sender.name, TodoStatus.WORK, args.argsMap[WORK_COMMENT] as String)
            sender.msg("<green>Work entry recorded.")
        }
    })
    editNameArg.then(LiteralArgument("work").then(workCommentArg))

    val node = LiteralArgument("edit")
    node.then(editNameArg)
    return node
}
