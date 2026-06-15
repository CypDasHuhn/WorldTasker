package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.ui.backItemBase
import dev.cypdashuhn.worldtasker.ui.primaryColor
import dev.cypdashuhn.worldtasker.ui.secondaryColor
import dev.cypdashuhn.worldtasker.ui.backgroundPane
import dev.cypdashuhn.worldtasker.ui.filters.FiltersContext
import dev.cypdashuhn.worldtasker.ui.filters.FiltersInterface
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.tags.TagQueryContext
import dev.cypdashuhn.worldtasker.ui.tags.TagSelectInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoListContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class NamespaceQueryContext(
    var filter: TodoFilter = TodoFilter(),
    val returnToFilters: Boolean = false,
) : ScrollContext()

object NamespaceSelectInterface : NamespaceOverviewBase<NamespaceQueryContext>(
    handler { NamespaceQueryContext() },
    ScrollInterfaceOptions<NamespaceQueryContext>().apply {
        inventoryTitle = { _, _ -> mm("${primaryColor}Filter ${secondaryColor}· Namespaces") }
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
            backItemBase()
                .onClick {
                    if (context.returnToFilters) {
                        FiltersInterface.openInventory(click.player, FiltersContext(context.filter))
                    } else {
                        TodoListInterface.openInventory(click.player, TodoListContext(filter = context.filter))
                    }
                },
            backgroundPane(),
        )
}
