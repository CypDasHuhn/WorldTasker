package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.db.TagFilterState
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.primaryColor
import dev.cypdashuhn.worldtasker.ui.secondaryColor
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceQueryContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceSelectInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import dev.rooster.ui.items.Slots
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class TagQueryContext(
    val namespaceId: Int,
    var filter: TodoFilter,
    val returnToFilters: Boolean = false,
) : ScrollContext()

object TagSelectInterface : TagOverviewBase<TagQueryContext>(
    "TagSelectInterface",
    handler { TagQueryContext(0, TodoFilter()) },
    ScrollInterfaceOptions<TagQueryContext>().apply {
        inventoryTitle = { _, _ -> mm("${primaryColor}Filter ${secondaryColor}· Tags") }
        sizeFromRows(3)
    },
) {
    override fun namespaceId(context: TagQueryContext) = context.namespaceId

    override fun contentDisplay(data: TagData, context: TagQueryContext,): InterfaceInfo<TagQueryContext>.() -> ItemStack =
        {
            val state = context.filter.stateOf(data.id)
            val (material, stateLabel, loreColor) =
                when (state) {
                    TagFilterState.NEUTRAL -> Triple(Material.GRAY_CONCRETE, "Neutral", "<gray>")
                    TagFilterState.INCLUDE -> Triple(Material.LIME_CONCRETE, "Include", "<green>")
                    TagFilterState.EXCLUDE -> Triple(Material.RED_CONCRETE, "Exclude", "<red>")
                }
            val lore =
                listOf(
                    mm("${loreColor}$stateLabel"),
                    mm("<dark_gray>Click to cycle: Neutral → Include → Exclude"),
                )
            createItem(material, mm("<white>${data.name}"), lore, additional = { meta ->
                if (state != TagFilterState.NEUTRAL) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true)
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
            })
        }

    override fun contentClick(data: TagData, context: TagQueryContext,): ClickInfo<TagQueryContext>.() -> Unit =
        {
            context.filter = context.filter.toggle(data.id)
            TagSelectInterface.openInventory(click.player, context)
        }

    override fun getInterfaceItems(): List<InterfaceItem<TagQueryContext>> =
        listOf(
            item()
                .atSlots(Slots(bottomRow..bottomRow + 2)),
        ) + backAndBackground(NamespaceSelectInterface) { NamespaceQueryContext(context.filter, context.returnToFilters) }
}
