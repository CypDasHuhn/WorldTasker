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

internal fun buildEditNode(): LiteralArgument {
    val editNameArg = StringArgument("editTodoName").suggestTodoNames()

    editNameArg.then(
        LiteralArgument("complete").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
                TodoManager.complete(id, sender.name)
                sender.msg("<green>Todo marked complete.")
            }
        })
    )

    editNameArg.then(
        LiteralArgument("reactivate").executesPlayer(PlayerCommandExecutor { sender, args ->
            handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
                TodoManager.reactivate(id, sender.name)
                sender.msg("<green>Todo reactivated.")
            }
        })
    )

    val newDescArg = TextArgument("editTodoNewDescription")
    newDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
            TodoManager.updateDescription(id, args.argsMap["editTodoNewDescription"] as String)
            sender.msg("<green>Description updated.")
        }
    })
    editNameArg.then(LiteralArgument("description").then(newDescArg))

    val editTagsNode = LiteralArgument("tags")

    val setTagsArg = TextArgument("editTodoSetTags").suggestTagNamesCommaText()
    setTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
            TagManager.removeAllForTodo(id)
            resolveTagIds(args.argsMap["editTodoSetTags"] as String, sender).forEach { TagManager.addToTodo(id, it) }
            sender.msg("<green>Tags set.")
        }
    })
    editTagsNode.then(LiteralArgument("set").then(setTagsArg))

    val addTagsArg = TextArgument("editTodoAddTags").suggestTagNamesCommaText()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
            resolveTagIds(args.argsMap["editTodoAddTags"] as String, sender).forEach { TagManager.addToTodo(id, it) }
            sender.msg("<green>Tags added.")
        }
    })
    editTagsNode.then(LiteralArgument("add").then(addTagsArg))

    val removeTagsArg = TextArgument("editTodoRemoveTags").suggestTagNamesCommaText()
    removeTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
            resolveTagIds(args.argsMap["editTodoRemoveTags"] as String, sender).forEach { TagManager.removeFromTodo(id, it) }
            sender.msg("<green>Tags removed.")
        }
    })
    editTagsNode.then(LiteralArgument("remove").then(removeTagsArg))

    editNameArg.then(editTagsNode)

    val workCommentArg = TextArgument("editTodoWorkComment")
    workCommentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleWithTodo(sender, args.argsMap["editTodoName"] as String) { id ->
            HistoryManager.record(id, sender.name, TodoStatus.WORK, args.argsMap["editTodoWorkComment"] as String)
            sender.msg("<green>Work entry recorded.")
        }
    })
    editNameArg.then(LiteralArgument("work").then(workCommentArg))

    val node = LiteralArgument("edit")
    node.then(editNameArg)
    return node
}
