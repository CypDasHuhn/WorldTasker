package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.ui.filters.FiltersContext
import dev.cypdashuhn.worldtasker.ui.filters.FiltersInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagQueryContext
import dev.cypdashuhn.worldtasker.ui.tags.TagSelectInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoListContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
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
import org.bukkit.inventory.ItemStack

data class NamespaceData(
    val id: Int,
    val name: String,
    val material: Material,
)

// ─── abstract base shared by all namespace overview modes ────────────────────

abstract class NamespaceOverviewBase<C : ScrollContext>(
    name: String,
    handler: ContextHandler<C>,
    options: ScrollInterfaceOptions<C>,
) : ScrollInterface<C, NamespaceData>(name, handler, options) {
    override fun contentProvider(id: Int, context: C): NamespaceData? =
        NamespaceManager.all().getOrNull(id)?.let {
            NamespaceData(
                it[NamespaceManager.Namespaces.id].value,
                it[NamespaceManager.Namespaces.name],
                Material.getMaterial(it[NamespaceManager.Namespaces.material]) ?: Material.BOOKSHELF,
            )
        }
}

// ─── query mode (tag filter selection) ───────────────────────────────────────

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

class NamespaceQueryContext(
    var filter: TodoFilter = TodoFilter(),
    val returnToFilters: Boolean = false,
) : ScrollContext()

object NamespaceSelectInterface : NamespaceOverviewBase<NamespaceQueryContext>(
    "NamespaceSelectInterface",
    handler { NamespaceQueryContext() },
    ScrollInterfaceOptions<NamespaceQueryContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Filter <gray>· Namespaces") }
        sizeFromRows(3)
    },
) {
    override fun contentDisplay(
        data: NamespaceData,
        context: NamespaceQueryContext,
    ): InterfaceInfo<NamespaceQueryContext>.() -> ItemStack =
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
            createItem(data.material, mm("<white>${data.name}"), lore)
        }

    override fun contentClick(data: NamespaceData, context: NamespaceQueryContext,): ClickInfo<NamespaceQueryContext>.() -> Unit =
        {
            TagSelectInterface.openInventory(click.player, TagQueryContext(data.id, context.filter, context.returnToFilters))
        }

    override fun getInterfaceItems(): List<InterfaceItem<NamespaceQueryContext>> =
        listOf(
            item()
                .atSlot(9 * 2)
                .displayAs(
                    createItem(
                        Material.FEATHER,
                        mm("<white>Back"),
                        listOf(mm("<gray>Apply filter and return.")),
                    ),
                ).onClick {
                    if (context.returnToFilters) {
                        FiltersInterface.openInventory(click.player, FiltersContext(context.filter))
                    } else {
                        TodoListInterface.openInventory(click.player, TodoListContext(filter = context.filter))
                    }
                },
        )
}
