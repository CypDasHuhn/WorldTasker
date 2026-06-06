package dev.cypdashuhn.worldtasker.ui.filters

import dev.cypdashuhn.worldtasker.WorldTaskerPlugin
import dev.cypdashuhn.worldtasker.db.TagFilterState
import dev.cypdashuhn.worldtasker.db.TodoFilter
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
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

private val miniMessage = MiniMessage.miniMessage()

private fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

class AuthorContext(
    var filter: TodoFilter = TodoFilter()
) : ScrollContext()

data class AuthorEntry(
    val name: String,
    val uuid: String
)

object AuthorInterface : ScrollInterface<AuthorContext, AuthorEntry>(
    "AuthorInterface",
    handler { AuthorContext() },
    ScrollInterfaceOptions<AuthorContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Filter <gray>· Authors") }
        sizeFromRows(4)
    },
) {
    private fun allPlayers(): List<AuthorEntry> =
        WorldTaskerPlugin.playerManager
            .players()
            .sortedBy { it.name }
            .map { AuthorEntry(it.name, it.uuid) }

    override fun contentProvider(id: Int, context: AuthorContext): AuthorEntry? = allPlayers().getOrNull(id)

    override fun contentDisplay(data: AuthorEntry, context: AuthorContext,): InterfaceInfo<AuthorContext>.() -> ItemStack =
        {
            val state = context.filter.authorStateOf(data.name)
            val (stateLabel, loreColor) = when (state) {
                TagFilterState.NEUTRAL -> "Neutral" to "<gray>"
                TagFilterState.INCLUDE -> "Include" to "<green>"
                TagFilterState.EXCLUDE -> "Exclude" to "<red>"
            }
            val skull = ItemStack(Material.PLAYER_HEAD)
            val meta = skull.itemMeta as SkullMeta
            meta.playerProfile = Bukkit.createProfile(UUID.fromString(data.uuid), data.name)
            meta.displayName(mm("<white>${data.name}"))
            meta.lore(
                listOf(
                    mm("${loreColor}$stateLabel"),
                    mm("<dark_gray>Click to cycle: Neutral → Include → Exclude"),
                ),
            )
            if (state != TagFilterState.NEUTRAL) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
            skull.itemMeta = meta
            skull
        }

    override fun contentClick(data: AuthorEntry, context: AuthorContext,): ClickInfo<AuthorContext>.() -> Unit =
        {
            context.filter = context.filter.toggleAuthor(data.name)
            AuthorInterface.openInventory(click.player, context)
        }

    override fun getInterfaceItems(): List<InterfaceItem<AuthorContext>> =
        listOf(
            item()
                .atSlot(bottomRow)
                .displayAs(
                    createItem(Material.FEATHER, mm("<white>Back"), listOf(mm("<gray>Return to filters."))),
                ).routeTo(FiltersInterface) { FiltersContext(context.filter) },
        )
}
