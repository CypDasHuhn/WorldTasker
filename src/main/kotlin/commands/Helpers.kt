package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoState
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

internal enum class TodoNameFilter { ACTIVE, COMPLETED, ALL }

internal fun TodoNameFilter.allowsState(state: TodoState): Boolean = when (this) {
    TodoNameFilter.ACTIVE -> state == TodoState.ACTIVE
    TodoNameFilter.COMPLETED -> state == TodoState.COMPLETED
    TodoNameFilter.ALL -> state != TodoState.DELETED
}

private val mm = MiniMessage.miniMessage()
internal fun Player.msg(text: String) = sendMessage(mm.deserialize(text))

internal fun handleWithTodo(sender: Player, name: String, filter: TodoNameFilter = TodoNameFilter.ACTIVE, block: (Int) -> Unit) {
    val todo = TodoManager.findByName(name)
    if (todo == null) {
        sender.msg("<red>Todo '<white>$name</white>' not found.")
        return
    }
    val id = todo[TodoManager.Todos.id].value
    if (!filter.allowsState(TodoManager.stateOf(id))) {
        sender.msg("<red>Todo '<white>$name</white>' not found.")
        return
    }
    block(id)
}

