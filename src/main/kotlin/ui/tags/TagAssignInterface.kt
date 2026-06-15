package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.primaryColor
import dev.cypdashuhn.worldtasker.ui.secondaryColor
import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagAssignResult
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceAssignContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceAssignInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.InterfaceInfo
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import dev.rooster.ui.interfaces.constructors.indexed_content.sizeFromRows
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.items.InterfaceItem
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.transactions.transaction

class TagAssignContext(
    val namespaceId: Int,
    val todoId: Int,
) : ScrollContext()

object TagAssignInterface : TagOverviewBase<TagAssignContext>(
    handler { TagAssignContext(0, 0) },
    ScrollInterfaceOptions<TagAssignContext>().apply {
        inventoryTitle = { _, context ->
            val nsName = NamespaceManager.find(context.namespaceId)?.get(NamespaceManager.Namespaces.name) ?: "?"
            mm("${primaryColor}Assign Tags ${secondaryColor}· $nsName")
        }
        sizeFromRows(3)
    },
) {
    override fun namespaceId(context: TagAssignContext) = context.namespaceId

    override fun contentDisplay(data: TagData, context: TagAssignContext,): InterfaceInfo<TagAssignContext>.() -> ItemStack =
        {
            val assigned = transaction {
                TagManager.tagsForTodo(context.todoId).any { it[TagManager.Tags.id].value == data.id }
            }
            val lore = listOf(mm(if (assigned) "<green>Assigned" else "<gray>Not assigned"))
            createItem(data.material, mm("<white>${data.name}"), lore, additional = { meta ->
                if (assigned) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true)
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
            })
        }

    override fun contentClick(data: TagData, context: TagAssignContext,): ClickInfo<TagAssignContext>.() -> Unit =
        {
            val assigned = transaction {
                TagManager.tagsForTodo(context.todoId).any { it[TagManager.Tags.id].value == data.id }
            }
            if (assigned) {
                TagManager.removeTagsFromTodo(context.todoId, listOf(data.id))
            } else {
                when (val result = TagManager.addToTodo(context.todoId, data.id)) {
                    TagAssignResult.Success -> { }

                    TagAssignResult.MultipleScopeTags -> {
                        click.player.msg("<red>A todo can only have one scope tag.")
                    }

                    is TagAssignResult.ScopeCollision -> {
                        click.player
                            .msg("<red>A todo named '<white>${result.todoName}</white>' scoped to '${result.scopeTagName}' already exists.")
                    }

                    is TagAssignResult.NamespaceSingleTagViolation -> {
                        val nsList = result.namespaceNames.joinToString(", ")
                        click.player
                            .msg("<red>This todo already has a tag from <white>$nsList</white> (single-tag namespace). Remove it first.")
                    }
                }
            }
            TagAssignInterface.openInventory(click.player, context)
        }

    override fun getInterfaceItems(): List<InterfaceItem<TagAssignContext>> =
        backAndBackground(NamespaceAssignInterface) { NamespaceAssignContext(context.todoId) }
}
