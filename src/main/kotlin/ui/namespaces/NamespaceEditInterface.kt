package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceCreateResult
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.primaryColor
import dev.cypdashuhn.worldtasker.ui.secondaryColor
import dev.cypdashuhn.worldtasker.ui.mm
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
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class NamespaceEditContext : ScrollContext()

object NamespaceEditInterface : NamespaceOverviewBase<NamespaceEditContext>(
    "NamespaceEditInterface",
    handler { NamespaceEditContext() },
    ScrollInterfaceOptions<NamespaceEditContext>().apply {
        inventoryTitle = { _, _ -> mm("${primaryColor}Edit ${secondaryColor}· Namespaces") }
        sizeFromRows(4)
    },
) {
    override fun contentDisplay(data: NamespaceData, context: NamespaceEditContext,): InterfaceInfo<NamespaceEditContext>.() -> ItemStack =
        {
            val tagCount = TagManager.byNamespace(data.id).size
            val mode = if (data.allowsMultiple) "multiple" else "single"
            createItem(data.material, mm("<white>${data.name}"), listOf(
                mm("<gray>$tagCount tag(s) · Tag mode: $mode"),
                mm("<dark_gray>Click to edit tags · Toggle mode in tag editor"),
            ))
        }

    override fun contentClick(data: NamespaceData, context: NamespaceEditContext,): ClickInfo<NamespaceEditContext>.() -> Unit =
        {
            TagEditInterface.openInventory(click.player, TagEditContext(data.id))
        }

    override fun getInterfaceItems(): List<InterfaceItem<NamespaceEditContext>> =
        backAndBackground(TodoListInterface) + listOf(
            item()
                .atSlot(bottomRow + 4)
                .displayAs(
                    createItem(
                        Material.WRITABLE_BOOK,
                        mm("${primaryColor}Add Namespace"),
                        listOf(mm("<gray>Type a new namespace name in chat.")),
                    ),
                ).onClick {
                    ChatInputManager.awaitInput(click.player, "<gray>Type the new namespace name:") { name ->
                        if (name.isNotBlank()) {
                            val trimmed = name.trim()
                            when (NamespaceManager.create(trimmed)) {
                                is NamespaceCreateResult.Created -> { }

                                NamespaceCreateResult.ReservedName -> {
                                    click.player.msg("<red>'<white>$trimmed</white>' is reserved and cannot be used.")
                                }

                                NamespaceCreateResult.DuplicateName -> {
                                    click.player.msg("<red>Namespace '<white>$trimmed</white>' already exists.")
                                }
                            }
                        }
                        NamespaceEditInterface.openInventory(click.player, NamespaceEditContext())
                    }
                },
        )
}
