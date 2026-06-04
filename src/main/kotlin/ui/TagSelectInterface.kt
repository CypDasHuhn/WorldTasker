package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.TagFilterState
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
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
)

class TagSelectContext(
    val namespaceId: Int,
    var filter: TodoFilter,
) : ScrollContext()

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

object TagSelectInterface : ScrollInterface<TagSelectContext, TagData>(
    "TagSelectInterface",
    handler { TagSelectContext(0, TodoFilter()) },
    ScrollInterfaceOptions<TagSelectContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Filter <gray>· Tags") }
        sizeFromRows(3)
    },
) {
    override fun contentProvider(
        id: Int,
        context: TagSelectContext,
    ): TagData? =
        TagManager.byNamespace(context.namespaceId).getOrNull(id)?.let {
            TagData(it[TagManager.Tags.id].value, it[TagManager.Tags.name])
        }

    override fun contentDisplay(
        data: TagData,
        context: TagSelectContext,
    ): InterfaceInfo<TagSelectContext>.() -> ItemStack =
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
        context: TagSelectContext,
    ): ClickInfo<TagSelectContext>.() -> Unit =
        {
            context.filter = context.filter.toggle(data.id)
            TagSelectInterface.openInventory(click.player, context)
        }

    override fun getOtherItems(): List<InterfaceItem<TagSelectContext>> =
        listOf(
            item()
                .atSlot(2 * 9)
                .displayAs(
                    createItem(
                        Material.FEATHER,
                        mm("<white>Back"),
                        listOf(mm("<gray>Return to namespace selection.")),
                    ),
                ).routeTo(NamespaceSelectInterface) { NamespaceSelectContext(context.filter) },
        )
}
