package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceQueryContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceSelectInterface
import dev.cypdashuhn.worldtasker.db.TagFilterState
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.ContextHandler
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

data class TagData(
    val id: Int,
    val name: String,
    val material: Material,
)

// ─── abstract base shared by all tag overview modes ──────────────────────────

private val miniMessage = MiniMessage.miniMessage()
private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

abstract class TagOverviewBase<C : ScrollContext>(
    name: String,
    handler: ContextHandler<C>,
    options: ScrollInterfaceOptions<C>,
) : ScrollInterface<C, TagData>(name, handler, options) {

    abstract fun namespaceId(context: C): Int

    override fun contentProvider(id: Int, context: C): TagData? =
        TagManager.byNamespace(namespaceId(context)).getOrNull(id)?.let {
            TagData(
                it[TagManager.Tags.id].value,
                it[TagManager.Tags.name],
                Material.getMaterial(it[TagManager.Tags.material]) ?: Material.PAPER,
            )
        }
}

// ─── query mode (tag filter selection) ───────────────────────────────────────

class TagQueryContext(
    val namespaceId: Int,
    var filter: TodoFilter,
    val returnToFilters: Boolean = false,
) : ScrollContext()

object TagSelectInterface : TagOverviewBase<TagQueryContext>(
    "TagSelectInterface",
    handler { TagQueryContext(0, TodoFilter()) },
    ScrollInterfaceOptions<TagQueryContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Filter <gray>· Tags") }
        sizeFromRows(3)
    },
) {
    override fun namespaceId(context: TagQueryContext) = context.namespaceId

    override fun contentDisplay(
        data: TagData,
        context: TagQueryContext,
    ): InterfaceInfo<TagQueryContext>.() -> ItemStack =
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

    override fun contentClick(
        data: TagData,
        context: TagQueryContext,
    ): ClickInfo<TagQueryContext>.() -> Unit =
        {
            context.filter = context.filter.toggle(data.id)
            TagSelectInterface.openInventory(click.player, context)
        }

    override fun getInterfaceItems(): List<InterfaceItem<TagQueryContext>> =
        listOf(
            item()
                .atSlot(2 * 9)
                .displayAs(
                    createItem(
                        Material.FEATHER,
                        mm("<white>Back"),
                        listOf(mm("<gray>Return to namespace selection.")),
                    ),
                ).routeTo(NamespaceSelectInterface) {
                    NamespaceQueryContext(context.filter, context.returnToFilters)
                },
        )
}
