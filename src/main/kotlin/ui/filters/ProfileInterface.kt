package dev.cypdashuhn.worldtasker.ui.filters

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.ProfileData
import dev.cypdashuhn.worldtasker.db.QueryProfileManager
import dev.cypdashuhn.worldtasker.db.StatusFilter
import dev.cypdashuhn.worldtasker.ui.backAndBackground
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.todo.TodoListContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
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
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class ProfileListContext : ScrollContext()

object ProfileListInterface : ScrollInterface<ProfileListContext, ProfileData>(
    "ProfileListInterface",
    handler { ProfileListContext() },
    ScrollInterfaceOptions<ProfileListContext>().apply {
        inventoryTitle = { _, _ -> mm("<white>Query Profiles") }
        sizeFromRows(4)
    },
) {
    override fun contentProvider(id: Int, context: ProfileListContext): ProfileData? = QueryProfileManager.all().getOrNull(id)

    override fun contentDisplay(data: ProfileData, context: ProfileListContext): InterfaceInfo<ProfileListContext>.() -> ItemStack =
        {
            val filter = data.filter
            val lore = buildList<TextComponent> {
                if (filter.included.isNotEmpty()) add(mm("<green>+ ${filter.included.size} included tag(s)"))
                if (filter.excluded.isNotEmpty()) add(mm("<red>- ${filter.excluded.size} excluded tag(s)"))
                val statusLabel = when (filter.statusFilter) {
                    StatusFilter.DEFAULT -> "active"
                    StatusFilter.ALL -> "all"
                    StatusFilter.COMPLETED -> "completed"
                }
                if (filter.statusFilter != StatusFilter.DEFAULT) add(mm("<yellow>Status: $statusLabel"))
                if (filter.authorIncluded.isNotEmpty()) add(mm("<aqua>+ ${filter.authorIncluded.size} author(s)"))
                if (filter.authorExcluded.isNotEmpty()) add(mm("<red>- ${filter.authorExcluded.size} author(s)"))
                if (filter.distanceEnabled) add(mm("<light_purple>Distance: ${filter.distanceRadius} blocks"))
                if (filter.isEmpty()) add(mm("<gray>Empty filter"))
            }
            createItem(
                if (filter.isEmpty()) Material.PAPER else Material.WRITTEN_BOOK,
                mm("<white>${data.name}"),
                lore,
            )
        }

    override fun contentClick(data: ProfileData, context: ProfileListContext): ClickInfo<ProfileListContext>.() -> Unit =
        {
            TodoListInterface.openInventory(click.player, TodoListContext(data.filter))
        }

    override fun getInterfaceItems(): List<InterfaceItem<ProfileListContext>> =
        backAndBackground(FiltersInterface) { FiltersContext() } + listOf(
            item()
                .atSlot(bottomRow + 4)
                .displayAs(
                    createItem(
                        Material.TNT,
                        mm("<red>Delete Profile"),
                        listOf(mm("<gray>Click to delete a profile by name.")),
                    ),
                ).onClick {
                    ChatInputManager.awaitInput(click.player, "<gray>Type the profile name to delete:") { name ->
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            ProfileListInterface.openInventory(click.player, ProfileListContext())
                            return@awaitInput
                        }
                        val profile = QueryProfileManager.findByName(trimmed)
                        if (profile == null) {
                            click.player.msg("<red>Profile '<white>$trimmed</white>' not found.")
                        } else {
                            QueryProfileManager.delete(profile[QueryProfileManager.QueryProfiles.id].value)
                            click.player.msg("<green>Profile '<white>$trimmed</white>' deleted.")
                        }
                        ProfileListInterface.openInventory(click.player, ProfileListContext())
                    }
                },
        )
}
