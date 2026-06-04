package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.db.TodoManager
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
import org.jetbrains.exposed.sql.transactions.transaction

data class TodoData(
    val id: Int,
    val name: String,
    val description: String,
    val tags: List<String>,
    val inheritedTags: List<String>,
)

class TodoListContext(
    var filter: TodoFilter = TodoFilter(),
) : ScrollContext()

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

object TodoListInterface : ScrollInterface<TodoListContext, TodoData>(
    "TodoListInterface",
    handler { TodoListContext() },
    ScrollInterfaceOptions<TodoListContext>().apply {
        inventoryTitle = { _, _ -> mm("<white><bold>Todos") }
        sizeFromRows(6)
    },
) {
    override fun contentProvider(
        id: Int,
        context: TodoListContext,
    ): TodoData? {
        val filter = context.filter
        val ids = TodoManager.filteredIds(filter)
        val todoId = ids.getOrNull(id) ?: return null
        return transaction {
            TodoManager.findById(todoId)?.let {
                TodoData(
                    id = todoId,
                    name = it.name,
                    description = it.description,
                    tags = TagManager.tagLabelsForTodo(todoId),
                    inheritedTags = TagManager.inheritedTagLabelsForTodo(todoId),
                )
            }
        }
    }

    override fun contentDisplay(
        data: TodoData,
        context: TodoListContext,
    ): InterfaceInfo<TodoListContext>.() -> ItemStack =
        {
            val lore =
                buildList<TextComponent> {
                    if (data.tags.isNotEmpty()) add(mm("<gray>Tags: <white>${data.tags.joinToString(", ")}"))
                    if (data.inheritedTags.isNotEmpty()) add(mm("<dark_gray>Inherited: ${data.inheritedTags.joinToString(", ")}"))
                    add(mm("<gray>\"${data.description}\""))
                }
            createItem(Material.WRITABLE_BOOK, mm("<white>${data.name}"), lore)
        }

    override fun contentClick(
        data: TodoData,
        context: TodoListContext,
    ): ClickInfo<TodoListContext>.() -> Unit =
        {
            TodoDetailInterface.openInventory(click.player, TodoDetailContext(data.id))
        }

    override fun getOtherItems(): List<InterfaceItem<TodoListContext>> =
        listOf(
            item()
                .atSlot(6, 0)
                .displayAs {
                    val filter = context.filter
                    val lore =
                        buildList<TextComponent> {
                            if (!filter.isEmpty()) {
                                if (filter.included.isNotEmpty()) add(mm("<green>${filter.included.size} included tag(s)"))
                                if (filter.excluded.isNotEmpty()) add(mm("<red>${filter.excluded.size} excluded tag(s)"))
                            } else {
                                add(mm("<gray>No filter active."))
                            }
                        }
                    createItem(
                        if (filter.isEmpty()) Material.HOPPER else Material.COMPARATOR,
                        mm("<white>Filter"),
                        lore,
                    )
                }.routeTo(NamespaceSelectInterface) { NamespaceSelectContext(context.filter) },
        )
}
