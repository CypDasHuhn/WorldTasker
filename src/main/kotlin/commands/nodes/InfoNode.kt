package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestTodoNames
import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val NAME = "infoTodoName"

internal fun buildInfoNode(): LiteralArgument {
    val nameArg = StringArgument(NAME).suggestTodoNames()
    nameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NAME] as String
        val todo = TodoManager.findByName(name)
        if (todo == null) {
            sender.msg("<red>Todo '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }

        val id = todo[TodoManager.Todos.id].value
        val author = todo[TodoManager.Todos.author]
        val description = todo[TodoManager.Todos.description]
        val tags = TagManager.tagLabelsForTodo(id)
        val inherited = TagManager.inheritedTagLabelsForTodo(id)
        val history = HistoryManager.forTodo(id)

        sender.msg("<gold>=== <white>$name <gold>===")
        sender.msg("<gray>Author: <white>$author")
        sender.msg("<gray>\"<white>$description<gray>\"")
        if (tags.isNotEmpty())
            sender.msg("<gray>Tags: <white>${tags.joinToString(", ")}")
        if (inherited.isNotEmpty())
            sender.msg("<gray>Inherited: <dark_gray>${inherited.joinToString(", ")}")

        sender.msg("<gold>--- History ---")
        history.forEach { entry ->
            val time = entry[HistoryManager.History.time].toLocalDate()
            val entryAuthor = entry[HistoryManager.History.author]
            val status = entry[HistoryManager.History.status]
            val comment = entry[HistoryManager.History.comment]
            val commentPart = if (comment != null) " <gray>— <white>$comment" else ""
            sender.msg("<dark_gray>$time <yellow>$status <gray>($entryAuthor)$commentPart")
        }
    })

    val node = LiteralArgument("info")
    node.then(nameArg)
    return node
}
