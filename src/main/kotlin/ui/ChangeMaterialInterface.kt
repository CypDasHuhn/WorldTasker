package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailContext
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagEditContext
import dev.cypdashuhn.worldtasker.ui.tags.TagEditInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.ContextHandler
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player

private val miniMessage = MiniMessage.miniMessage()
private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

private const val MATERIAL_SLOT = 13  // center slot of a 3-row inventory

// ─── abstract base ────────────────────────────────────────────────────────────

abstract class ChangeMaterialBase<C : Context>(
    name: String,
    handler: ContextHandler<C>,
    titleFn: (Player, C) -> Component,
) : RoosterInterface<C>(
    name,
    handler,
    options {
        inventorySize = InventorySize.THREE_ROWS
        inventoryTitle = titleFn
        cancelEvent = { info ->
            info.click.slot != MATERIAL_SLOT &&
                info.click.event.clickedInventory != info.click.event.view.bottomInventory
        }
    },
) {
    abstract fun onSave(info: ClickInfo<C>, material: Material)

    override fun getInterfaceItems(): List<InterfaceItem<C>> =
        listOf(
            item()
                .atSlots((0..2).map { it * 9 }.flatMap { outer -> (3..5).map { it -> it + outer }}.filter { it != MATERIAL_SLOT })
                .displayAs(createItem(Material.GRAY_STAINED_GLASS_PANE, mm(""), listOf())),
            item()
                .atSlot(bottomRow + 8)
                .displayAs(
                    createItem(
                        Material.LIME_STAINED_GLASS_PANE,
                        mm("<green>Save"),
                        listOf(mm("<gray>Saves the item in the center slot as the material.")),
                    ),
                ).onClick {
                    val newMaterial = click.player.openInventory.topInventory.getItem(MATERIAL_SLOT)?.type
                    if (newMaterial != null && newMaterial != Material.AIR) {
                        onSave(this, newMaterial)
                    }
                },
        )
}

// ─── tag material change ──────────────────────────────────────────────────────

class ChangeTagMaterialContext(
    val tagId: Int,
    val namespaceId: Int,
) : Context()

object ChangeTagMaterialInterface : ChangeMaterialBase<ChangeTagMaterialContext>(
    "ChangeTagMaterialInterface",
    handler { ChangeTagMaterialContext(0, 0) },
    { _, context ->
        val name = TagManager.find(context.tagId)?.get(TagManager.Tags.name) ?: "Tag"
        mm("<white>Change Material <gray>· $name")
    },
) {
    override fun onSave(info: ClickInfo<ChangeTagMaterialContext>, material: Material) {
        TagManager.updateMaterial(info.context.tagId, material.name)
        TagDetailInterface.openInventory(info.click.player, TagDetailContext(info.context.tagId, info.context.namespaceId))
    }
}

// ─── namespace material change ────────────────────────────────────────────────

class ChangeNamespaceMaterialContext(
    val namespaceId: Int,
) : Context()

object ChangeNamespaceMaterialInterface : ChangeMaterialBase<ChangeNamespaceMaterialContext>(
    "ChangeNamespaceMaterialInterface",
    handler { ChangeNamespaceMaterialContext(0) },
    { _, context ->
        val name = NamespaceManager.find(context.namespaceId)?.get(NamespaceManager.Namespaces.name) ?: "Namespace"
        mm("<white>Change Material <gray>· $name")
    },
) {
    override fun onSave(info: ClickInfo<ChangeNamespaceMaterialContext>, material: Material) {
        NamespaceManager.updateMaterial(info.context.namespaceId, material.name)
        TagEditInterface.openInventory(info.click.player, TagEditContext(info.context.namespaceId))
    }
}
