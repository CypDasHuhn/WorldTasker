package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceInheritanceContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceInheritanceInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class InheritedTagData(
    val parentTagId: Int,
    val name: String,
    val material: Material,
    val nsName: String
)

class TagInheritanceContext(
    val tagId: Int,
    val namespaceId: Int,
) : ScrollContext()

object TagInheritanceInterface : ScrollInterface<TagInheritanceContext, InheritedTagData>(
    "TagInheritanceInterface",
    handler { TagInheritanceContext(0, 0) },
    ScrollInterfaceOptions<TagInheritanceContext>().apply {
        inventoryTitle = { _, context ->
            val name = TagManager.find(context.tagId)?.get(TagManager.Tags.name) ?: "Tag"
            mm("<white>$name <gray>· Inheritance")
        }
        sizeFromRows(4)
    },
) {
    override fun contentProvider(id: Int, context: TagInheritanceContext): InheritedTagData? {
        val parents = TagManager.parentsOf(context.tagId)
        return parents.getOrNull(id)?.let { row ->
            val nsName = NamespaceManager
                .find(row[TagManager.Tags.namespaceId].value)
                ?.get(NamespaceManager.Namespaces.name) ?: "?"
            InheritedTagData(
                parentTagId = row[TagManager.Tags.id].value,
                name = row[TagManager.Tags.name],
                material = Material.getMaterial(row[TagManager.Tags.material]) ?: Material.PAPER,
                nsName = nsName,
            )
        }
    }

    override fun contentDisplay(
        data: InheritedTagData,
        context: TagInheritanceContext,
    ): InterfaceInfo<TagInheritanceContext>.() -> ItemStack =
        {
            createItem(
                data.material,
                mm("<white>${data.nsName}:${data.name}"),
                listOf(mm("<red>Click to remove this inheritance.")),
            )
        }

    override fun contentClick(data: InheritedTagData, context: TagInheritanceContext,): ClickInfo<TagInheritanceContext>.() -> Unit =
        {
            TagManager.removeInheritance(context.tagId, data.parentTagId)
            TagInheritanceInterface.openInventory(click.player, context)
        }

    override fun getInterfaceItems(): List<InterfaceItem<TagInheritanceContext>> =
        listOf(
            item()
                .atSlot(bottomRow)
                .displayAs(
                    createItem(Material.FEATHER, mm("<white>Back"), listOf(mm("<gray>Return to tag detail."))),
                ).routeTo(TagDetailInterface) { TagDetailContext(context.tagId, context.namespaceId) },
            item()
                .atSlot(bottomRow + 8)
                .displayAs(
                    createItem(
                        Material.WRITABLE_BOOK,
                        mm("<white>Add Parent Tag"),
                        listOf(mm("<gray>Browse namespaces to add a parent.")),
                    ),
                ).routeTo(NamespaceInheritanceInterface) { NamespaceInheritanceContext(context.tagId, context.namespaceId) },
        )
}

// ─── tag selection step for inheritance ──────────────────────────────────────

class TagInheritanceSelectContext(
    val namespaceId: Int,
    val childTagId: Int,
    val childNsId: Int,
) : ScrollContext()

object TagInheritanceSelectInterface : TagOverviewBase<TagInheritanceSelectContext>(
    "TagInheritanceSelectInterface",
    handler { TagInheritanceSelectContext(0, 0, 0) },
    ScrollInterfaceOptions<TagInheritanceSelectContext>().apply {
        inventoryTitle = { _, context ->
            val nsName = NamespaceManager.find(context.namespaceId)?.get(NamespaceManager.Namespaces.name) ?: "?"
            mm("<white>Add Inheritance <gray>· $nsName")
        }
        sizeFromRows(3)
    },
) {
    override fun namespaceId(context: TagInheritanceSelectContext) = context.namespaceId

    override fun contentDisplay(
        data: TagData,
        context: TagInheritanceSelectContext,
    ): InterfaceInfo<TagInheritanceSelectContext>.() -> ItemStack =
        {
            val isParent = TagManager.parentsOf(context.childTagId).any { it[TagManager.Tags.id].value == data.id }
            val lore = listOf(mm(if (isParent) "<green>Already a parent" else "<gray>Click to add as parent"))
            createItem(data.material, mm("<white>${data.name}"), lore)
        }

    override fun contentClick(data: TagData, context: TagInheritanceSelectContext,): ClickInfo<TagInheritanceSelectContext>.() -> Unit =
        {
            if (data.id != context.childTagId) {
                TagManager.addInheritance(context.childTagId, data.id)
            }
            TagInheritanceInterface.openInventory(
                click.player,
                TagInheritanceContext(context.childTagId, context.childNsId),
            )
        }

    override fun getInterfaceItems(): List<InterfaceItem<TagInheritanceSelectContext>> =
        listOf(
            item()
                .atSlot(2 * 9)
                .displayAs(createItem(Material.FEATHER, mm("<white>Back"), listOf(mm("<gray>Return to namespace."))))
                .routeTo(NamespaceInheritanceInterface) {
                    NamespaceInheritanceContext(context.childTagId, context.childNsId)
                },
        )
}
