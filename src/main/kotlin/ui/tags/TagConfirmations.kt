package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TagRenameResult
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.player
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.constructors.confirmation.BaseConfirmationInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class DeleteTagContext(val tagId: Int, val namespaceId: Int) : Context()

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

class RenameTagContext(val tagId: Int, val namespaceId: Int) : Context()

object RenameTagConfirmation : BaseConfirmationInterface<RenameTagContext>(
    "RenameTagConfirmation",
    handler { RenameTagContext(0, 0) },
    onConfirm = { info ->
        val player = info.click.player
        val tagId = info.context.tagId
        val nsId = info.context.namespaceId
        ChatInputManager.awaitInput(player, "<gray>Type the new tag name:") { newName ->
            if (newName.isNotBlank()) {
                val trimmed = newName.trim()
                when (TagManager.rename(tagId, trimmed)) {
                    TagRenameResult.RENAMED -> { }
                    TagRenameResult.RESERVED_NAME ->
                        player.msg("<red>'<white>$trimmed</white>' is reserved and cannot be used.")
                    TagRenameResult.DUPLICATE_NAME ->
                        player.msg("<red>A tag named '<white>$trimmed</white>' already exists in this namespace.")
                }
            }
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
            item().atSlot(4).displayAs(createItem(Material.SHEARS, mm("<yellow>Todos are kept. This tag will be removed from them."))),
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
