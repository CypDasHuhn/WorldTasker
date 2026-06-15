package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagCreateResult
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.ChangeNamespaceMaterialContext
import dev.cypdashuhn.worldtasker.ui.ChangeNamespaceMaterialInterface
import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.namespaces.DeleteNamespaceConfirmation
import dev.cypdashuhn.worldtasker.ui.namespaces.DeleteNamespaceContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.RenameNamespaceConfirmation
import dev.cypdashuhn.worldtasker.ui.namespaces.RenameNamespaceContext
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

class TagEditContext(
    val namespaceId: Int
) : ScrollContext()

object TagEditInterface : TagOverviewBase<TagEditContext>(
    "TagEditInterface",
    handler { TagEditContext(0) },
    ScrollInterfaceOptions<TagEditContext>().apply {
        inventoryTitle = { _, context ->
            val nsName = NamespaceManager.find(context.namespaceId)?.get(NamespaceManager.Namespaces.name) ?: "?"
            mm("<white>Edit <gray>· $nsName")
        }
        sizeFromRows(4)
    },
) {
    override fun namespaceId(context: TagEditContext) = context.namespaceId

    override fun contentDisplay(data: TagData, context: TagEditContext,): InterfaceInfo<TagEditContext>.() -> ItemStack =
        {
            val ancestors = TagManager.ancestorLabelsOf(data.id)
            val lore = buildList<TextComponent> {
                if (ancestors.isNotEmpty()) {
                    add(mm("<gray>Inherits:"))
                    ancestors.forEach { add(mm("<dark_gray>  $it")) }
                }
            }
            createItem(data.material, mm("<white>${data.name}"), lore)
        }

    override fun contentClick(data: TagData, context: TagEditContext,): ClickInfo<TagEditContext>.() -> Unit =
        {
            TagDetailInterface.openInventory(click.player, TagDetailContext(data.id, context.namespaceId))
        }

    override fun getInterfaceItems(): List<InterfaceItem<TagEditContext>> =
        backAndBackground(NamespaceEditInterface) { NamespaceEditContext() } + listOf(
            item()
                .atSlot(bottomRow + 2)
                .displayAs {
                    val ns = NamespaceManager.find(context.namespaceId)
                    val allowsMultiple = ns?.get(NamespaceManager.Namespaces.allowsMultiple) ?: true
                    val mode = if (allowsMultiple) "multiple" else "single"
                    createItem(
                        if (allowsMultiple) Material.OAK_SIGN else Material.IRON_DOOR,
                        mm("<white>Tag Mode: $mode"),
                        listOf(mm("<gray>Click to toggle between single/multiple tag assignment.")),
                    )
                }.onClick {
                    val ns = NamespaceManager.find(context.namespaceId) ?: return@onClick
                    val current = ns[NamespaceManager.Namespaces.allowsMultiple]
                    ToggleTagModeConfirmation.openInventory(click.player, ToggleTagModeContext(context.namespaceId, !current))
                },
            item()
                .atSlot(bottomRow + 3)
                .displayAs {
                    createItem(Material.BOOKSHELF, mm("<white>Change Material"), listOf(mm("<gray>Change the namespace's display material.")))
                }.routeTo(ChangeNamespaceMaterialInterface) { ChangeNamespaceMaterialContext(context.namespaceId) },
            item()
                .atSlot(bottomRow + 4)
                .displayAs { createItem(Material.WRITABLE_BOOK, mm("<white>Add Tag"), listOf(mm("<gray>Type a new tag name."))) }
                .onClick {
                    val nsId = context.namespaceId
                    ChatInputManager.awaitInput(click.player, "<gray>Type the new tag name:") { name ->
                        if (name.isNotBlank()) {
                            val trimmed = name.trim()
                            when (TagManager.create(trimmed, nsId)) {
                                is TagCreateResult.Created -> { }

                                TagCreateResult.ReservedName -> {
                                    click.player.msg("<red>'<white>$trimmed</white>' is reserved and cannot be used.")
                                }

                                TagCreateResult.DuplicateName -> {
                                    click.player.msg("<red>Tag '<white>$trimmed</white>' already exists in this namespace.")
                                }
                            }
                        }
                        TagEditInterface.openInventory(click.player, TagEditContext(nsId))
                    }
                },
            item()
                .atSlot(bottomRow + 5)
                .displayAs { createItem(Material.TNT, mm("<red>Delete Namespace"), listOf(mm("<gray>Requires confirmation."))) }
                .onClick {
                    if (TagManager.byNamespace(context.namespaceId).isNotEmpty()) {
                        click.player.msg("<red>Cannot delete a namespace that still has tags.")
                        return@onClick
                    }
                    DeleteNamespaceConfirmation.openInventory(click.player, DeleteNamespaceContext(context.namespaceId))
                },
            item()
                .atSlot(bottomRow + 7)
                .displayAs { createItem(Material.NAME_TAG, mm("<white>Rename Namespace"), listOf(mm("<gray>Requires confirmation."))) }
                .routeTo(RenameNamespaceConfirmation) { RenameNamespaceContext(context.namespaceId) },
        )
}
