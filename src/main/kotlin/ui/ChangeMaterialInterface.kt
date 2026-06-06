package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailContext
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagEditContext
import dev.cypdashuhn.worldtasker.ui.tags.TagEditInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material

private val miniMessage = MiniMessage.miniMessage()
private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

private const val MATERIAL_SLOT = 13  // center slot of a 3-row inventory

// ─── tag material change ──────────────────────────────────────────────────────

class ChangeTagMaterialContext(
    val tagId: Int,
    val namespaceId: Int,
) : Context()

object ChangeTagMaterialInterface : RoosterInterface<ChangeTagMaterialContext>(
    "ChangeTagMaterialInterface",
    handler { ChangeTagMaterialContext(0, 0) },
    options {
        inventorySize = InventorySize.THREE_ROWS
        inventoryTitle = { _, context ->
            val name = TagManager.find(context.tagId)?.get(TagManager.Tags.name) ?: "Tag"
            mm("<white>Change Material <gray>· $name")
        }
        cancelEvent = { info -> info.click.slot != MATERIAL_SLOT }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<ChangeTagMaterialContext>> =
        listOf(
            item()
                .atSlot(bottomRow + 8)
                .displayAs(
                    createItem(
                        Material.LIME_STAINED_GLASS_PANE,
                        mm("<green>Save"),
                        listOf(mm("<gray>Saves the item in the center slot as the tag material.")),
                    ),
                ).onClick {
                    val newMaterial = click.player.openInventory.topInventory.getItem(MATERIAL_SLOT)?.type
                    if (newMaterial != null && newMaterial != Material.AIR) {
                        TagManager.updateMaterial(context.tagId, newMaterial.name)
                    }
                    TagDetailInterface.openInventory(click.player, TagDetailContext(context.tagId, context.namespaceId))
                },
        )
}

// ─── namespace material change ────────────────────────────────────────────────

class ChangeNamespaceMaterialContext(
    val namespaceId: Int,
) : Context()

object ChangeNamespaceMaterialInterface : RoosterInterface<ChangeNamespaceMaterialContext>(
    "ChangeNamespaceMaterialInterface",
    handler { ChangeNamespaceMaterialContext(0) },
    options {
        inventorySize = InventorySize.THREE_ROWS
        inventoryTitle = { _, context ->
            val name = NamespaceManager.find(context.namespaceId)?.get(NamespaceManager.Namespaces.name) ?: "Namespace"
            mm("<white>Change Material <gray>· $name")
        }
        cancelEvent = { info -> info.click.slot != MATERIAL_SLOT }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<ChangeNamespaceMaterialContext>> =
        listOf(
            item()
                .atSlot(bottomRow + 8)
                .displayAs(
                    createItem(
                        Material.LIME_STAINED_GLASS_PANE,
                        mm("<green>Save"),
                        listOf(mm("<gray>Saves the item in the center slot as the namespace material.")),
                    ),
                ).onClick {
                    val newMaterial = click.player.openInventory.topInventory.getItem(MATERIAL_SLOT)?.type
                    if (newMaterial != null && newMaterial != Material.AIR) {
                        NamespaceManager.updateMaterial(context.namespaceId, newMaterial.name)
                    }
                    TagEditInterface.openInventory(click.player, TagEditContext(context.namespaceId))
                },
        )
}
