package dev.cypdashuhn.worldtasker.ui.todo

import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.player
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.constructors.confirmation.BaseConfirmationInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

object DeleteTodoConfirmation : BaseConfirmationInterface<TodoDetailContext>(
    "DeleteTodoConfirmation",
    handler { TodoDetailContext(0) },
    onConfirm = { info ->
        TodoManager.delete(info.context.todoId, info.click.player)
        TodoListInterface.openInventory(info.click.player)
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TodoDetailInterface.openInventory(player, TodoDetailContext(info.context.todoId))
    },
) {
    override fun getInventory(player: Player, context: TodoDetailContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<red>Delete Todo?"))

    override fun getOtherItems(): List<InterfaceItem<TodoDetailContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.BARRIER, mm("<red>This todo will be deleted forever."))),
        )
}

object RenameTodoConfirmation : BaseConfirmationInterface<TodoDetailContext>(
    "RenameTodoConfirmation",
    handler { TodoDetailContext(0) },
    onConfirm = { info ->
        val player = info.click.player
        val todoId = info.context.todoId
        ChatInputManager.awaitInput(player, "<gray>Type the new todo name:") { newName ->
            if (newName.isNotBlank()) TodoManager.updateName(todoId, newName.trim())
            TodoDetailInterface.openInventory(player, TodoDetailContext(todoId))
        }
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TodoDetailInterface.openInventory(player, TodoDetailContext(info.context.todoId))
    },
) {
    override fun getInventory(player: Player, context: TodoDetailContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<yellow>Rename Todo?"))

    override fun getOtherItems(): List<InterfaceItem<TodoDetailContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.NAME_TAG, mm("<yellow>You will be prompted to type the new name."))),
        )
}
