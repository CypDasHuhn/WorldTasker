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

internal fun buildAddNode(): LiteralArgument {
    val addTagsArg = TextArgument("addTodoTags").suggestTagNamesCommaText()
    addTagsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleTodoAdd(
            sender,
            args.argsMap["addTodoName"] as String,
            args.argsMap["addTodoDescription"] as String,
            args.argsMap["addTodoTags"] as String,
        )
    })

    val addDescArg = TextArgument("addTodoDescription")
    addDescArg.then(addTagsArg)
    addDescArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        handleTodoAdd(
            sender,
            args.argsMap["addTodoName"] as String,
            args.argsMap["addTodoDescription"] as String,
            null,
        )
    })

    val addNameArg = StringArgument("addTodoName")
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
