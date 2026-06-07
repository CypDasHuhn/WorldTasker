package dev.cypdashuhn.worldtasker.ui.todo

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoCreateResult
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.filters.FiltersContext
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.filters.FiltersInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditInterface
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
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.jetbrains.exposed.sql.transactions.transaction

data class TodoData(
    val id: Int,
    val name: String,
    val description: String,
    val author: String,
    val tags: List<String>,
    val inheritedTags: List<String>,
    val scopeTag: String?,
)

class TodoListContext(
    var filter: TodoFilter = TodoFilter(),
) : ScrollContext()

object TodoListInterface : ScrollInterface<TodoListContext, TodoData>(
    "TodoListInterface",
    handler { TodoListContext() },
    ScrollInterfaceOptions<TodoListContext>().apply {
        inventoryTitle = { _, _ -> mm("<white><bold>Todos") }
        sizeFromRows(6)
    },
) {
    override fun contentProvider(id: Int, context: TodoListContext,): TodoData? {
        val filter = context.filter
        val ids = TodoManager.filteredIds(filter)
        val todoId = ids.getOrNull(id) ?: return null
        return transaction {
            TodoManager.findById(todoId)?.let {
                TodoData(
                    id = todoId,
                    name = it.name,
                    description = it.description,
                    author = it.author,
                    tags = TagManager.tagLabelsForTodo(todoId),
                    inheritedTags = TagManager.inheritedTagLabelsForTodo(todoId),
                    scopeTag = TodoScopeManager.scopeTagNameForTodo(todoId),
                )
            }
        }
    }

    override fun contentDisplay(data: TodoData, context: TodoListContext,): InterfaceInfo<TodoListContext>.() -> ItemStack =
        {
            val lore =
                buildList<TextComponent> {
                    if (data.tags.isNotEmpty()) add(mm("<gray>Tags: <white>${data.tags.joinToString(", ")}"))
                    if (data.inheritedTags.isNotEmpty()) add(mm("<dark_gray>Inherited: ${data.inheritedTags.joinToString(", ")}"))
                    add(mm("<gray>\"${data.description}\""))
                }
            val skull = ItemStack(Material.PLAYER_HEAD)
            val meta = skull.itemMeta as SkullMeta
            @Suppress("DEPRECATION")
            meta.owningPlayer = Bukkit.getOfflinePlayer(data.author)
            val displayName = if (data.scopeTag != null) "${data.scopeTag}:${data.name}" else data.name
            meta.displayName(mm("<white>$displayName"))
            meta.lore(lore)
            skull.itemMeta = meta
            skull
        }

    override fun contentClick(data: TodoData, context: TodoListContext,): ClickInfo<TodoListContext>.() -> Unit =
        {
            TodoDetailInterface.openInventory(click.player, TodoDetailContext(data.id))
        }

    override fun getInterfaceItems() =
        listOf(
            // Filters button → FiltersInterface
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
                        mm("<white>Filters"),
                        lore,
                    )
                }.routeTo(FiltersInterface) { FiltersContext(context.filter) },
            // New todo button
            item()
                .atSlot(6, 4)
                .displayAs(
                    createItem(
                        Material.WRITABLE_BOOK,
                        mm("<white>New Todo"),
                        listOf(mm("<gray>Create a new todo.")),
                    ),
                ).onClick {
                    val player = click.player
                    ChatInputManager.awaitInput(player, "<gray>Type the todo <white>name<gray>:") { name ->
                        if (name.isBlank()) {
                            TodoListInterface.openInventory(player, context)
                            return@awaitInput
                        }
                        ChatInputManager.awaitInput(player, "<gray>Type the todo <white>description<gray>:") { description ->
                            val trimmedName = name.trim()
                            when (val result = TodoManager.create(trimmedName, player, description.trim(), player.location)) {
                                is TodoCreateResult.Created -> { }
                                TodoCreateResult.MultipleScopeTags ->
                                    player.msg("<red>A todo can only have one scope tag.")
                                is TodoCreateResult.ScopeCollision -> {
                                    val scopeDesc = if (result.scopeTagName != null)
                                        "scoped to '<white>${result.scopeTagName}</white>'"
                                    else "with no scope tag"
                                    player.msg("<red>A todo named '<white>$trimmedName</white>' $scopeDesc already exists.")
                                }
                            }
                            TodoListInterface.openInventory(player, context)
                        }
                    }
                },
            // Namespace edit button
            item()
                .atSlot(6, 8)
                .displayAs(
                    createItem(
                        Material.BOOKSHELF,
                        mm("<white>Namespaces"),
                        listOf(mm("<gray>Manage namespaces and tags.")),
                    ),
                ).routeTo(NamespaceEditInterface) { NamespaceEditContext() },
            // Random todo button
            item()
                .atSlot(6, 7)
                .displayAs(
                    createItem(
                        Material.ENDER_EYE,
                        mm("<white>Random Todo"),
                        listOf(mm("<gray>Open a random todo from the current list.")),
                    ),
                ).onClick {
                    val ids = TodoManager.filteredIds(context.filter)
                    if (ids.isEmpty()) return@onClick
                    val randomId = ids.random()
                    TodoDetailInterface.openInventory(click.player, TodoDetailContext(randomId))
                },
        )
}
