package dev.cypdashuhn.worldtasker.ui.todo

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.primaryColor
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceAssignContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceAssignInterface
import dev.rooster.core.util.createItem
import dev.rooster.db.utility_tables.LocationManager
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.jetbrains.exposed.sql.transactions.transaction

class TodoDetailContext(
    val todoId: Int,
) : Context()

object TodoDetailInterface : RoosterInterface<TodoDetailContext>(
    handler { TodoDetailContext(0) },
    options {
        inventorySize = InventorySize.TWO_ROWS
        inventoryTitle = { _, context ->
            val name = transaction { TodoManager.findById(context.todoId)?.name } ?: "Todo"
            val scopeTag = TodoScopeManager.scopeTagNameForTodo(context.todoId)
            val title = if (scopeTag != null) "$scopeTag:$name" else name
            mm("${primaryColor}$title")
        }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<TodoDetailContext>> =
        backAndBackground(TodoListInterface) + listOf(
            // Jump to location
            item()
                .atSlot(4)
                .usedWhen { transaction { TodoManager.findById(context.todoId)?.locationId } != null }
                .displayAs(
                    createItem(
                        Material.ENDER_PEARL,
                        mm("${primaryColor}Jump To"),
                        listOf(mm("<gray>Teleport to this todo's location.")),
                    ),
                ).onClick {
                    val todo = transaction { TodoManager.findById(context.todoId) } ?: return@onClick
                    val locId = todo.locationId ?: return@onClick
                    val location = transaction { LocationManager.Location.findById(locId)?.location() } ?: return@onClick
                    click.player.teleport(location)
                },
            // Tag button — shows current tags in lore, opens assign mode
            item()
                .atSlot(6)
                .displayAs {
                    val direct = TagManager.tagLabelsForTodo(context.todoId)
                    val inherited = TagManager.inheritedTagLabelsForTodo(context.todoId)
                    val lore = buildList<TextComponent> {
                        if (direct.isNotEmpty()) add(mm("<gray>Tags: <white>${direct.joinToString(", ")}"))
                        if (inherited.isNotEmpty()) add(mm("<dark_gray>Inherited: ${inherited.joinToString(", ")}"))
                        if (direct.isEmpty() && inherited.isEmpty()) add(mm("<gray>No tags assigned."))
                        add(mm("<dark_gray>Click to manage tags."))
                    }
                    createItem(Material.NAME_TAG, mm("${primaryColor}Tags"), lore)
                }.routeTo(NamespaceAssignInterface) { NamespaceAssignContext(context.todoId) },
            // Rename button
            item()
                .atSlot(0)
                .displayAs(
                    createItem(
                        Material.OAK_SIGN,
                        mm("${primaryColor}Rename"),
                        listOf(mm("<gray>Requires confirmation.")),
                    ),
                ).routeTo(RenameTodoConfirmation) { TodoDetailContext(context.todoId) },
            // Delete button
            item()
                .atSlot(2)
                .displayAs(
                    createItem(
                        Material.TNT,
                        mm("<red>Delete"),
                        listOf(mm("<gray>Requires confirmation.")),
                    ),
                ).routeTo(DeleteTodoConfirmation) { TodoDetailContext(context.todoId) },
            // History button
            item()
                .atSlot(8)
                .displayAs(
                    createItem(
                        Material.CLOCK,
                        mm("${primaryColor}History"),
                        listOf(mm("<gray>View the history of this todo.")),
                    ),
                ).routeTo(TodoHistoryInterface) { TodoHistoryContext(context.todoId) },
        )
}
