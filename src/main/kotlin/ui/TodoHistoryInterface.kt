package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoStatus
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
import java.time.LocalDateTime

data class HistoryEntry(
    val status: TodoStatus,
    val comment: String?,
    val author: String,
    val time: LocalDateTime,
)

class TodoHistoryContext(
    val todoId: Int,
) : ScrollContext()

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

object TodoHistoryInterface : ScrollInterface<TodoHistoryContext, HistoryEntry>(
    "TodoHistoryInterface",
    handler { TodoHistoryContext(0) },
    ScrollInterfaceOptions<TodoHistoryContext>().apply {
        scrollDirection = ScrollDirection.LEFT_RIGHT
        sizeFromRows(2)
        inventoryTitle = { _, context ->
            val name = transaction { TodoManager.findById(context.todoId)?.name } ?: "Todo"
            mm("<white>$name <gray>· History")
        }
    },
) {
    override fun contentProvider(
        id: Int,
        context: TodoHistoryContext,
    ): HistoryEntry? {
        val entries = HistoryManager.forTodo(context.todoId)
        return entries.getOrNull(id)?.let {
            HistoryEntry(
                status = it[HistoryManager.History.status],
                comment = it[HistoryManager.History.comment],
                author = it[HistoryManager.History.author],
                time = it[HistoryManager.History.time],
            )
        }
    }

    override fun contentDisplay(
        data: HistoryEntry,
        context: TodoHistoryContext,
    ): InterfaceInfo<TodoHistoryContext>.() -> ItemStack =
        {
            val material =
                when (data.status) {
                    TodoStatus.CREATE -> Material.LIME_CONCRETE
                    TodoStatus.WORK -> Material.LIGHT_BLUE_CONCRETE
                    TodoStatus.COMPLETE -> Material.RED_CONCRETE
                    TodoStatus.REACTIVATE -> Material.YELLOW_CONCRETE
                    TodoStatus.DELETE -> Material.BLACK_CONCRETE
                }
            val stateName =
                data.status.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            val lore =
                buildList {
                    add(mm("<dark_gray>${data.author} · ${data.time.toLocalDate()}"))
                    data.comment?.let { add(mm("<gray>\"$it\"")) }
                }
            createItem(material, mm("<white>$stateName"), lore)
        }

    override fun contentClick(
        data: HistoryEntry,
        context: TodoHistoryContext,
    ): ClickInfo<TodoHistoryContext>.() -> Unit = { }

    override fun getOtherItems(): List<InterfaceItem<TodoHistoryContext>> =
        listOf(
            item()
                .atSlot(bottomRow)
                .displayAs(
                    createItem(
                        Material.FEATHER,
                        mm("<white>Back"),
                        listOf(mm("<gray>Return to todo detail.")),
                    ),
                ).routeTo(TodoDetailInterface) { TodoDetailContext(context.todoId) },
        )
}
