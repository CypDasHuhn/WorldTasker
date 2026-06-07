package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceDeleteResult
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.NamespaceRenameResult
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.player
import dev.cypdashuhn.worldtasker.ui.tags.TagEditContext
import dev.cypdashuhn.worldtasker.ui.tags.TagEditInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.constructors.confirmation.BaseConfirmationInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class DeleteNamespaceContext(val namespaceId: Int) : Context()

object DeleteNamespaceConfirmation : BaseConfirmationInterface<DeleteNamespaceContext>(
    "DeleteNamespaceConfirmation",
    handler { DeleteNamespaceContext(0) },
    onConfirm = { info ->
        val player = info.click.player
        when (NamespaceManager.delete(info.context.namespaceId)) {
            NamespaceDeleteResult.DELETED ->
                NamespaceEditInterface.openInventory(player, NamespaceEditContext())
            NamespaceDeleteResult.BLOCKED_SCOPE -> {
                player.msg("<red>This namespace is the active todo scope and cannot be deleted.")
                NamespaceEditInterface.openInventory(player, NamespaceEditContext())
            }
            NamespaceDeleteResult.BLOCKED_HAS_TAGS -> {
                player.msg("<red>This namespace still has tags. Remove all tags first.")
                NamespaceEditInterface.openInventory(player, NamespaceEditContext())
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
            item().atSlot(4).displayAs(createItem(Material.BARRIER, mm("<red>This namespace will be permanently deleted."))),
        )
}

class RenameNamespaceContext(val namespaceId: Int) : Context()

object RenameNamespaceConfirmation : BaseConfirmationInterface<RenameNamespaceContext>(
    "RenameNamespaceConfirmation",
    handler { RenameNamespaceContext(0) },
    onConfirm = { info ->
        val player = info.click.player
        val nsId = info.context.namespaceId
        ChatInputManager.awaitInput(player, "<gray>Type the new namespace name:") { newName ->
            if (newName.isNotBlank()) {
                val trimmed = newName.trim()
                when (NamespaceManager.rename(nsId, trimmed)) {
                    NamespaceRenameResult.RENAMED -> { }
                    NamespaceRenameResult.RESERVED_NAME ->
                        player.msg("<red>'<white>$trimmed</white>' is reserved and cannot be used.")
                    NamespaceRenameResult.DUPLICATE_NAME ->
                        player.msg("<red>Namespace '<white>$trimmed</white>' already exists.")
                }
            }
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
