package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.tags.TagInheritanceContext
import dev.cypdashuhn.worldtasker.ui.tags.TagInheritanceInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagInheritanceSelectContext
import dev.cypdashuhn.worldtasker.ui.tags.TagInheritanceSelectInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class NamespaceInheritanceContext(
    val childTagId: Int,
    val childNsId: Int,
) : ScrollContext()

object NamespaceInheritanceInterface : NamespaceOverviewBase<NamespaceInheritanceContext>(
    "NamespaceInheritanceInterface",
    handler { NamespaceInheritanceContext(0, 0) },
    ScrollInterfaceOptions<NamespaceInheritanceContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Add Inheritance <gray>· Namespaces") }
        sizeFromRows(3)
    },
) {
    override fun contentDisplay(
        data: NamespaceData,
        context: NamespaceInheritanceContext,
    ): InterfaceInfo<NamespaceInheritanceContext>.() -> ItemStack =
        {
            val tagCount = TagManager.byNamespace(data.id).size
            createItem(data.material, mm("<white>${data.name}"), listOf(mm("<gray>$tagCount tag(s)")))
        }

    override fun contentClick(
        data: NamespaceData,
        context: NamespaceInheritanceContext,
    ): ClickInfo<NamespaceInheritanceContext>.() -> Unit =
        {
            TagInheritanceSelectInterface.openInventory(
                click.player,
                TagInheritanceSelectContext(data.id, context.childTagId, context.childNsId),
            )
        }

    override fun getInterfaceItems(): List<InterfaceItem<NamespaceInheritanceContext>> =
        listOf(
            item()
                .atSlot(9 * 2)
                .displayAs(createItem(Material.FEATHER, mm("<white>Back"), listOf(mm("<gray>Return."))))
                .routeTo(TagInheritanceInterface) { TagInheritanceContext(context.childTagId, context.childNsId) },
        )
}
