package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.tags.TagEditContext
import dev.cypdashuhn.worldtasker.ui.tags.TagEditInterface
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
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

private val miniMessage = MiniMessage.miniMessage()
private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

class NamespaceEditContext : ScrollContext()

object NamespaceEditInterface : NamespaceOverviewBase<NamespaceEditContext>(
    "NamespaceEditInterface",
    handler { NamespaceEditContext() },
    ScrollInterfaceOptions<NamespaceEditContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Edit <gray>· Namespaces") }
        sizeFromRows(4)
    },
) {
    override fun contentDisplay(
        data: NamespaceData,
        context: NamespaceEditContext,
    ): InterfaceInfo<NamespaceEditContext>.() -> ItemStack =
        {
            val tagCount = TagManager.byNamespace(data.id).size
            createItem(data.material, mm("<white>${data.name}"), listOf(mm("<gray>$tagCount tag(s)")))
        }

    override fun contentClick(
        data: NamespaceData,
        context: NamespaceEditContext,
    ): ClickInfo<NamespaceEditContext>.() -> Unit =
        {
            TagEditInterface.openInventory(click.player, TagEditContext(data.id))
        }

    override fun getInterfaceItems(): List<InterfaceItem<NamespaceEditContext>> =
        listOf(
            item()
                .atSlot(bottomRow)
                .displayAs(
                    createItem(Material.FEATHER, mm("<white>Back"), listOf(mm("<gray>Return."))),
                ).routeTo(TodoListInterface),
            item()
                .atSlot(bottomRow + 8)
                .displayAs(
                    createItem(
                        Material.WRITABLE_BOOK,
                        mm("<white>Add Namespace"),
                        listOf(mm("<gray>Type a new namespace name in chat.")),
                    ),
                ).onClick {
                    ChatInputManager.awaitInput(click.player, "<gray>Type the new namespace name:") { name ->
                        if (name.isNotBlank()) NamespaceManager.create(name.trim())
                        NamespaceEditInterface.openInventory(click.player, NamespaceEditContext())
                    }
                },
        )
}
