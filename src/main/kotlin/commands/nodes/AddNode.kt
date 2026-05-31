package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.resolveTagIds
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesCommaText
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.entity.Player

private const val NAME        = "addTodoName"
private const val DESCRIPTION = "addTodoDescription"
private const val TAGS        = "addTodoTags"

internal fun buildAddNode(): LiteralArgument {
    val addTagsArg = TextArgument(TAGS).suggestTagNamesCommaText()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleTodoAdd(
            sender,
            args.argsMap[NAME] as String,
            args.argsMap[DESCRIPTION] as String,
            args.argsMap[TAGS] as String,
        )
    })

    val addDescArg = TextArgument(DESCRIPTION)
    addDescArg.then(addTagsArg)
    addDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleTodoAdd(
            sender,
            args.argsMap[NAME] as String,
            args.argsMap[DESCRIPTION] as String,
            null,
        )
    })

    val addNameArg = StringArgument(NAME)
    addNameArg.then(addDescArg)
    val node = LiteralArgument("add")
    node.then(addNameArg)
    return node
}

private fun handleTodoAdd(sender: Player, name: String, description: String, tagsStr: String?) {
    val id = TodoManager.create(name, sender.name, description, sender.location)
    if (tagsStr != null) resolveTagIds(tagsStr, sender).forEach { TagManager.addToTodo(id, it) }
    sender.msg("<green>Todo '<white>$name</white>' created (id $id).")
}
