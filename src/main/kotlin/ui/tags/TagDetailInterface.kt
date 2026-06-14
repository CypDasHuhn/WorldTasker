package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.ChangeTagMaterialContext
import dev.cypdashuhn.worldtasker.ui.ChangeTagMaterialInterface
import dev.cypdashuhn.worldtasker.ui.backItem
import dev.cypdashuhn.worldtasker.ui.mm
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.jetbrains.exposed.sql.transactions.transaction

class TagDetailContext(
    val tagId: Int,
    val namespaceId: Int,
) : Context()

object TagDetailInterface : RoosterInterface<TagDetailContext>(
    "TagDetailInterface",
    handler { TagDetailContext(0, 0) },
    options {
        inventorySize = InventorySize.TWO_ROWS
        inventoryTitle = { _, context ->
            val nsName = NamespaceManager.find(context.namespaceId)?.get(NamespaceManager.Namespaces.name) ?: "?"
            val tagName = TagManager.find(context.tagId)?.get(TagManager.Tags.name) ?: "Tag"
            mm("<white>$nsName<gray>:$tagName")
        }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<TagDetailContext>> =
        listOf(
            backItem(TagEditInterface) { TagEditContext(context.namespaceId) },
            item()
                .atSlot(2)
                .displayAs(
                    createItem(
                        Material.COMPARATOR,
                        mm("<white>Change Material"),
                        listOf(mm("<gray>Change the icon for this tag.")),
                    ),
                ).routeTo(ChangeTagMaterialInterface) { ChangeTagMaterialContext(context.tagId, context.namespaceId) },
            item()
                .atSlot(4)
                .displayAs {
                    val parents = TagManager.parentsOf(context.tagId)
                    val lore = buildList<TextComponent> {
                        add(mm("<gray>${parents.size} parent tag(s)"))
                        parents.forEach { p ->
                            val pNsName = NamespaceManager
                                .find(p[TagManager.Tags.namespaceId].value)
                                ?.get(NamespaceManager.Namespaces.name) ?: "?"
                            add(mm("<dark_gray>$pNsName:${p[TagManager.Tags.name]}"))
                        }
                    }
                    createItem(Material.CHAIN, mm("<white>Inheritance"), lore)
                }.routeTo(TagInheritanceInterface) { TagInheritanceContext(context.tagId, context.namespaceId) },
            item()
                .atSlot(6)
                .displayAs(
                    createItem(
                        Material.NAME_TAG,
                        mm("<white>Rename"),
                        listOf(mm("<gray>Requires confirmation.")),
                    ),
                ).routeTo(RenameTagConfirmation) { RenameTagContext(context.tagId, context.namespaceId) },
            item()
                .atSlot(8)
                .displayAs(
                    createItem(
                        Material.TNT,
                        mm("<red>Delete Tag"),
                        listOf(mm("<gray>Requires confirmation.")),
                    ),
                ).onClick {
                    val ctx = DeleteTagContext(context.tagId, context.namespaceId)
                    if (TagManager.todosForTag(context.tagId).isEmpty()) {
                        DeleteTagConfirmation.openInventory(click.player, ctx)
                    } else {
                        TagDeleteConflictInterface.openInventory(click.player, ctx)
                    }
                },
        )
}
