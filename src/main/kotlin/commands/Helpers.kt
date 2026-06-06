package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.cypdashuhn.worldtasker.db.TodoResolveResult
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.db.TodoState
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

enum class TodoNameFilter { ACTIVE, COMPLETED, ALL }

internal fun TodoNameFilter.allowsState(state: TodoState): Boolean = when (this) {
    TodoNameFilter.ACTIVE -> state == TodoState.ACTIVE
    TodoNameFilter.COMPLETED -> state == TodoState.COMPLETED
    TodoNameFilter.ALL -> state != TodoState.DELETED
}

private val mm = MiniMessage.miniMessage()
internal fun Player.msg(text: String) = sendMessage(mm.deserialize(text))

internal fun la(name: String) = LiteralArgument(name)

internal fun LiteralArgument.withFilters(build: (TodoNameFilter) -> Argument<*>): LiteralArgument =
    apply {
        then(build(TodoNameFilter.ACTIVE))
        then(la("--completed").then(build(TodoNameFilter.COMPLETED)))
        then(la("--all").then(build(TodoNameFilter.ALL)))
    }

internal fun TodoNameFilter.toStates(): Set<TodoState> = when (this) {
    TodoNameFilter.ACTIVE -> setOf(TodoState.ACTIVE)
    TodoNameFilter.COMPLETED -> setOf(TodoState.COMPLETED)
    TodoNameFilter.ALL -> setOf(TodoState.ACTIVE, TodoState.COMPLETED)
}

internal fun handleWithScopedTodo(
    sender: Player,
    key: NamespacedKey,
    filter: TodoNameFilter = TodoNameFilter.ACTIVE,
    block: (id: Int, name: String) -> Unit,
) {
    when (val result = TodoScopeManager.resolveInput(key, filter.toStates())) {
        is TodoResolveResult.Found -> block(result.id, result.name)
        is TodoResolveResult.NotFound -> sender.msg("<red>Todo not found.")
        is TodoResolveResult.Ambiguous -> {
            sender.msg("<red>Multiple todos named '<white>${result.todoName}</white>'.")
            if (result.scopedOptions.isNotEmpty()) {
                sender.msg("<gray>Specify one of:")
                result.scopedOptions.forEach { sender.msg("<yellow>  $it") }
                if (result.hasUntagged) sender.msg("<gray>  (one match has no scope tag — assign one to disambiguate)")
            } else {
                sender.msg("<gray>No scope namespace configured — cannot disambiguate.")
            }
        }
    }
}

internal fun handleWithTodo(
    sender: Player,
    name: String,
    filter: TodoNameFilter = TodoNameFilter.ACTIVE,
    block: (Int) -> Unit
) {
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

