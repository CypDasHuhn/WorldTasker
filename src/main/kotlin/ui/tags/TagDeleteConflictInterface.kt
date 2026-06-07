package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.ui.DeleteTagContext
import dev.cypdashuhn.worldtasker.ui.RemoveTaggingsDeleteTagConfirmation
import dev.cypdashuhn.worldtasker.ui.RemoveTodosDeleteTagConfirmation
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

object TagDeleteConflictInterface : RoosterInterface<DeleteTagContext>(
    "TagDeleteConflictInterface",
    handler { DeleteTagContext(0, 0) },
    options {
        inventorySize = InventorySize.ONE_ROW
        inventoryTitle = { _, context ->
            val count = TagManager.todosForTag(context.tagId).size
            mm("<red>$count todo(s) tagged with this tag")
        }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<DeleteTagContext>> =
        listOf(
            item()
                .atSlot(0)
                .displayAs(
                    createItem(Material.BARRIER, mm("<white>Cancel"), listOf(mm("<gray>Return to tag."))),
                ).routeTo(TagDetailInterface) { TagDetailContext(context.tagId, context.namespaceId) },
            item()
                .atSlot(4)
                .displayAs {
                    val todos = TagManager.todosForTag(context.tagId)
                    val lore = todos.map { row -> mm("<gray>${row[TodoManager.Todos.name]}") }
                    createItem(Material.WRITABLE_BOOK, mm("<white>Tagged Todos"), lore)
                },
            item()
                .atSlot(6)
                .displayAs(
                    createItem(
                        Material.SHEARS,
                        mm("<yellow>Remove taggings, delete this tag"),
                        listOf(mm("<gray>Todos are kept. This tag is removed from them and then deleted.")),
                    ),
                ).routeTo(RemoveTaggingsDeleteTagConfirmation) { DeleteTagContext(context.tagId, context.namespaceId) },
            item()
                .atSlot(8)
                .displayAs(
                    createItem(
                        Material.TNT,
                        mm("<red>Remove todos, delete this tag"),
                        listOf(mm("<gray>All todos tagged with this tag are deleted, then the tag is deleted.")),
                    ),
                ).routeTo(RemoveTodosDeleteTagConfirmation) { DeleteTagContext(context.tagId, context.namespaceId) },
        )
}
