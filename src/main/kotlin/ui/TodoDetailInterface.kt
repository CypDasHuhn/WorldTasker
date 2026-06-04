package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.rooster.core.util.createItem
import dev.rooster.db.utility_tables.LocationManager
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.jetbrains.exposed.sql.transactions.transaction

class TodoDetailContext(
    val todoId: Int,
) : Context()

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

object TodoDetailInterface : RoosterInterface<TodoDetailContext>(
    "TodoDetailInterface",
    handler { TodoDetailContext(0) },
    options {
        inventorySize = InventorySize.TWO_ROWS
        inventoryTitle = { _, context ->
            val name = transaction { TodoManager.findById(context.todoId)?.name } ?: "Todo"
            mm("<white>$name")
        }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<TodoDetailContext>> =
        listOf(
            item()
                .atSlot(9)
                .displayAs(
                    createItem(
                        Material.FEATHER,
                        mm("<white>Back"),
                        listOf(mm("<gray>Return to the todo list.")),
                    ),
                ).routeTo(TodoListInterface),
            item()
                .atSlot(4)
                .usedWhen { transaction { TodoManager.findById(context.todoId)?.locationId } != null }
                .displayAs(
                    createItem(
                        Material.ENDER_PEARL,
                        mm("<white>Jump To"),
                        listOf(mm("<gray>Teleport to this todo's location.")),
                    ),
                ).onClick {
                    val todo = transaction { TodoManager.findById(context.todoId) } ?: return@onClick
                    val locId = todo.locationId ?: return@onClick
                    val location = transaction { LocationManager.Location.findById(locId)?.location() } ?: return@onClick
                    click.player.teleport(location)
                },
            item()
                .atSlot(8)
                .displayAs(
                    createItem(
                        Material.CLOCK,
                        mm("<white>History"),
                        listOf(mm("<gray>View the history of this todo.")),
                    ),
                ).routeTo(TodoHistoryInterface) { TodoHistoryContext(context.todoId) },
        )
}
