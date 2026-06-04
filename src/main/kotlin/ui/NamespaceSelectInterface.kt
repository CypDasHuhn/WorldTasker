package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.NamespaceManager
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
import org.bukkit.inventory.ItemStack

data class NamespaceData(
    val id: Int,
    val name: String,
)

class NamespaceSelectContext(
    val filter: TodoFilter,
) : ScrollContext()

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

object NamespaceSelectInterface : ScrollInterface<NamespaceSelectContext, NamespaceData>(
    "NamespaceSelectInterface",
    handler { NamespaceSelectContext(TodoFilter()) },
    ScrollInterfaceOptions<NamespaceSelectContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Filter <gray>· Namespaces") }
        sizeFromRows(3)
    },
) {
    override fun contentProvider(
        id: Int,
        context: NamespaceSelectContext,
    ): NamespaceData? =
        NamespaceManager.all().getOrNull(id)?.let {
            NamespaceData(it[NamespaceManager.Namespaces.id].value, it[NamespaceManager.Namespaces.name])
        }

    override fun contentDisplay(
        data: NamespaceData,
        context: NamespaceSelectContext,
    ): InterfaceInfo<NamespaceSelectContext>.() -> ItemStack =
        {
            val tagCount = TagManager.byNamespace(data.id).size
            val activeCount =
                context.filter.included.count { tagId ->
                    TagManager.find(tagId)?.get(TagManager.Tags.namespaceId)?.value == data.id
                } +
                    context.filter.excluded.count { tagId ->
                        TagManager.find(tagId)?.get(TagManager.Tags.namespaceId)?.value == data.id
                    }
            val lore =
                buildList<TextComponent> {
                    add(mm("<gray>$tagCount tag(s)"))
                    if (activeCount > 0) add(mm("<yellow>$activeCount active filter(s)"))
                }
            createItem(Material.BOOKSHELF, mm("<white>${data.name}"), lore)
        }

    override fun contentClick(
        data: NamespaceData,
        context: NamespaceSelectContext,
    ): ClickInfo<NamespaceSelectContext>.() -> Unit =
        {
            TagSelectInterface.openInventory(click.player, TagSelectContext(data.id, context.filter))
        }

    override fun getOtherItems(): List<InterfaceItem<NamespaceSelectContext>> =
        listOf(
            item()
                .atSlot(9 * 2)
                .displayAs(
                    createItem(
                        Material.FEATHER,
                        mm("<white>Back"),
                        listOf(mm("<gray>Apply filter and return to todo list.")),
                    ),
                ).routeTo(TodoListInterface) { TodoListContext(filter = context.filter) },
        )
}
