package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.tags.TagAssignContext
import dev.cypdashuhn.worldtasker.ui.tags.TagAssignInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoDetailContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoDetailInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class NamespaceAssignContext(
    val todoId: Int,
) : ScrollContext()

object NamespaceAssignInterface : NamespaceOverviewBase<NamespaceAssignContext>(
    "NamespaceAssignInterface",
    handler { NamespaceAssignContext(0) },
    ScrollInterfaceOptions<NamespaceAssignContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Assign Tags <gray>· Namespaces") }
        sizeFromRows(3)
    },
) {
    override fun contentDisplay(
        data: NamespaceData,
        context: NamespaceAssignContext,
    ): InterfaceInfo<NamespaceAssignContext>.() -> ItemStack =
        {
            val tagCount = TagManager.byNamespace(data.id).size
            createItem(data.material, mm("<white>${data.name}"), listOf(mm("<gray>$tagCount tag(s)")))
        }

    override fun contentClick(data: NamespaceData, context: NamespaceAssignContext,): ClickInfo<NamespaceAssignContext>.() -> Unit =
        {
            TagAssignInterface.openInventory(click.player, TagAssignContext(data.id, context.todoId))
        }

    override fun getInterfaceItems(): List<InterfaceItem<NamespaceAssignContext>> =
        backAndBackground(TodoDetailInterface) { TodoDetailContext(context.todoId) }
}
