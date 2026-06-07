package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceDeleteResult
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagDeleteConflictInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailContext
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagEditContext
import dev.cypdashuhn.worldtasker.ui.tags.TagEditInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoDetailContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoDetailInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.constructors.confirmation.BaseConfirmationInterface
import dev.rooster.ui.interfaces.constructors.confirmation.CancelInfo
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

private fun <T : Context> CancelInfo<T>.player(): Player? =
    cancelEvent.clickInfo?.second?.player
        ?: cancelEvent.closeInfo?.player as? Player

// ─── Delete Todo ──────────────────────────────────────────────────────────────

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

// ─── Rename Todo ──────────────────────────────────────────────────────────────

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

// ─── Delete Namespace ─────────────────────────────────────────────────────────

class DeleteNamespaceContext(
    val namespaceId: Int
) : Context()

object DeleteNamespaceConfirmation : BaseConfirmationInterface<DeleteNamespaceContext>(
    "DeleteNamespaceConfirmation",
    handler { DeleteNamespaceContext(0) },
    onConfirm = { info ->
        when (NamespaceManager.delete(info.context.namespaceId)) {
            NamespaceDeleteResult.DELETED -> {
                NamespaceEditInterface.openInventory(info.click.player, NamespaceEditContext())
            }

            NamespaceDeleteResult.BLOCKED_SCOPE -> {
                info.click.player.msg("<red>This namespace is the active todo scope and cannot be deleted.")
                NamespaceEditInterface.openInventory(info.click.player, NamespaceEditContext())
            }
        }
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TagEditInterface.openInventory(player, TagEditContext(info.context.namespaceId))
    },
) {
    override fun getInventory(player: Player, context: DeleteNamespaceContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<red>Delete Namespace?"))

    override fun getOtherItems(): List<InterfaceItem<DeleteNamespaceContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.BARRIER, mm("<red>All tags in this namespace will also be deleted."))),
        )
}

// ─── Rename Namespace ─────────────────────────────────────────────────────────

class RenameNamespaceContext(
    val namespaceId: Int
) : Context()

object RenameNamespaceConfirmation : BaseConfirmationInterface<RenameNamespaceContext>(
    "RenameNamespaceConfirmation",
    handler { RenameNamespaceContext(0) },
    onConfirm = { info ->
        val player = info.click.player
        val nsId = info.context.namespaceId
        ChatInputManager.awaitInput(player, "<gray>Type the new namespace name:") { newName ->
            if (newName.isNotBlank()) NamespaceManager.rename(nsId, newName.trim())
            TagEditInterface.openInventory(player, TagEditContext(nsId))
        }
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TagEditInterface.openInventory(player, TagEditContext(info.context.namespaceId))
    },
) {
    override fun getInventory(player: Player, context: RenameNamespaceContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<yellow>Rename Namespace?"))

    override fun getOtherItems(): List<InterfaceItem<RenameNamespaceContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.NAME_TAG, mm("<yellow>You will be prompted to type the new name."))),
        )
}

// ─── Delete Tag ───────────────────────────────────────────────────────────────

class DeleteTagContext(
    val tagId: Int,
    val namespaceId: Int
) : Context()

object DeleteTagConfirmation : BaseConfirmationInterface<DeleteTagContext>(
    "DeleteTagConfirmation",
    handler { DeleteTagContext(0, 0) },
    onConfirm = { info ->
        TagManager.delete(info.context.tagId)
        TagEditInterface.openInventory(info.click.player, TagEditContext(info.context.namespaceId))
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TagDetailInterface.openInventory(player, TagDetailContext(info.context.tagId, info.context.namespaceId))
    },
) {
    override fun getInventory(player: Player, context: DeleteTagContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<red>Delete Tag?"))

    override fun getOtherItems(): List<InterfaceItem<DeleteTagContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.BARRIER, mm("<red>This tag will be removed from all todos."))),
        )
}

// ─── Delete Tag: conflict resolution ─────────────────────────────────────────

object RemoveTaggingsDeleteTagConfirmation : BaseConfirmationInterface<DeleteTagContext>(
    "RemoveTaggingsDeleteTagConfirmation",
    handler { DeleteTagContext(0, 0) },
    onConfirm = { info ->
        TagManager.delete(info.context.tagId)
        TagEditInterface.openInventory(info.click.player, TagEditContext(info.context.namespaceId))
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TagDeleteConflictInterface.openInventory(player, DeleteTagContext(info.context.tagId, info.context.namespaceId))
    },
) {
    override fun getInventory(player: Player, context: DeleteTagContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<yellow>Remove taggings and delete tag?"))

    override fun getOtherItems(): List<InterfaceItem<DeleteTagContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.SHEARS, mm("<yellow>Todos will be kept. This tag will be removed from them."))),
        )
}

object RemoveTodosDeleteTagConfirmation : BaseConfirmationInterface<DeleteTagContext>(
    "RemoveTodosDeleteTagConfirmation",
    handler { DeleteTagContext(0, 0) },
    onConfirm = { info ->
        val player = info.click.player
        val tagId = info.context.tagId
        TagManager.todosForTag(tagId).forEach { row ->
            TodoManager.delete(row[TodoManager.Todos.id].value, player)
        }
        TagManager.delete(tagId)
        TagEditInterface.openInventory(player, TagEditContext(info.context.namespaceId))
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TagDeleteConflictInterface.openInventory(player, DeleteTagContext(info.context.tagId, info.context.namespaceId))
    },
) {
    override fun getInventory(player: Player, context: DeleteTagContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<red>Delete all tagged todos and tag?"))

    override fun getOtherItems(): List<InterfaceItem<DeleteTagContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.TNT, mm("<red>All todos tagged with this tag will be permanently deleted."))),
        )
}

// ─── Rename Tag ───────────────────────────────────────────────────────────────

class RenameTagContext(
    val tagId: Int,
    val namespaceId: Int
) : Context()

object RenameTagConfirmation : BaseConfirmationInterface<RenameTagContext>(
    "RenameTagConfirmation",
    handler { RenameTagContext(0, 0) },
    onConfirm = { info ->
        val player = info.click.player
        val tagId = info.context.tagId
        val nsId = info.context.namespaceId
        ChatInputManager.awaitInput(player, "<gray>Type the new tag name:") { newName ->
            if (newName.isNotBlank()) TagManager.rename(tagId, newName.trim())
            TagDetailInterface.openInventory(player, TagDetailContext(tagId, nsId))
        }
    },
    onCancel = { info ->
        val player = info.player()
        if (player != null) TagDetailInterface.openInventory(player, TagDetailContext(info.context.tagId, info.context.namespaceId))
    },
) {
    override fun getInventory(player: Player, context: RenameTagContext): Inventory =
        Bukkit.createInventory(null, 9, mm("<yellow>Rename Tag?"))

    override fun getOtherItems(): List<InterfaceItem<RenameTagContext>> =
        listOf(
            item().atSlot(4).displayAs(createItem(Material.NAME_TAG, mm("<yellow>You will be prompted to type the new name."))),
        )
}
