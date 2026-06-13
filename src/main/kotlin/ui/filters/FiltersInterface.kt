package dev.cypdashuhn.worldtasker.ui.filters

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.ProfileSaveResult
import dev.cypdashuhn.worldtasker.db.QueryProfileManager
import dev.cypdashuhn.worldtasker.db.StatusFilter
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.mm
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceQueryContext
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceSelectInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoListContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.InventorySize
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.handler
import dev.rooster.ui.interfaces.options
import dev.rooster.ui.items.InterfaceItem
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material

class FiltersContext(
    var filter: TodoFilter = TodoFilter()
) : Context()

object FiltersInterface : RoosterInterface<FiltersContext>(
    "FiltersInterface",
    handler { FiltersContext() },
    options {
        inventorySize = InventorySize.THREE_ROWS
        inventoryTitle = { _, _ -> mm("<white>Filters") }
    },
) {
    override fun getInterfaceItems(): List<InterfaceItem<FiltersContext>> =
        listOf(
            // Back → TodoList with current filter
            item()
                .atSlot(bottomRow)
                .displayAs(createItem(Material.FEATHER, mm("<white>Back"), listOf(mm("<gray>Return to todo list."))))
                .onClick {
                    TodoListInterface.openInventory(click.player, TodoListContext(filter = context.filter))
                },
            // Tag filter button → namespace query mode
            item()
                .atSlot(2)
                .displayAs {
                    val f = context.filter
                    val lore = buildList {
                        if (f.included.isNotEmpty()) add(mm("<green>${f.included.size} included"))
                        if (f.excluded.isNotEmpty()) add(mm("<red>${f.excluded.size} excluded"))
                        if (f.included.isEmpty() && f.excluded.isEmpty()) add(mm("<gray>No tag filter active."))
                    }
                    createItem(
                        if (f.included.isEmpty() && f.excluded.isEmpty()) Material.BOOKSHELF else Material.COMPARATOR,
                        mm("<white>Tag Filter"),
                        lore,
                    )
                }.onClick {
                    NamespaceSelectInterface.openInventory(
                        click.player,
                        NamespaceQueryContext(context.filter, returnToFilters = true),
                    )
                },
            // Status cycle button
            item()
                .atSlot(4)
                .displayAs {
                    val (material, label) = when (context.filter.statusFilter) {
                        StatusFilter.DEFAULT -> Material.LIME_CONCRETE to "Default (active only)"
                        StatusFilter.ALL -> Material.YELLOW_CONCRETE to "All (active + completed)"
                        StatusFilter.COMPLETED -> Material.ORANGE_CONCRETE to "Completed only"
                    }
                    createItem(material, mm("<white>Status: $label"), listOf(mm("<gray>Click to cycle.")))
                }.onClick {
                    val next = when (context.filter.statusFilter) {
                        StatusFilter.DEFAULT -> StatusFilter.ALL
                        StatusFilter.ALL -> StatusFilter.COMPLETED
                        StatusFilter.COMPLETED -> StatusFilter.DEFAULT
                    }
                    context.filter = context.filter.copy(statusFilter = next)
                    FiltersInterface.openInventory(click.player, context)
                },
            // Author filter button
            item()
                .atSlot(6)
                .displayAs {
                    val f = context.filter
                    val lore = buildList<TextComponent> {
                        if (f.authorIncluded.isNotEmpty()) add(mm("<green>${f.authorIncluded.size} included author(s)"))
                        if (f.authorExcluded.isNotEmpty()) add(mm("<red>${f.authorExcluded.size} excluded author(s)"))
                        if (f.authorIncluded.isEmpty() && f.authorExcluded.isEmpty()) add(mm("<gray>No author filter active."))
                    }
                    createItem(Material.PLAYER_HEAD, mm("<white>Author Filter"), lore)
                }.routeTo(AuthorInterface) { AuthorContext(context.filter) },
            // Distance filter button
            item()
                .atSlot(bottomRow + 4)
                .displayAs {
                    val f = context.filter
                    val lore = buildList<TextComponent> {
                        if (f.distanceEnabled) {
                            add(mm("<green>Active · radius ${f.distanceRadius} blocks"))
                            add(mm("<gray>Left/Right: adjust radius · Shift: toggle"))
                        } else {
                            add(mm("<gray>Off · Shift-click to enable"))
                        }
                    }
                    createItem(
                        if (context.filter.distanceEnabled) Material.ENDER_PEARL else Material.COMPASS,
                        mm("<white>Distance Filter"),
                        lore,
                    )
                }.onClick {
                    val f = context.filter
                    when {
                        event.click.isShiftClick -> {
                            context.filter = if (f.distanceEnabled) {
                                f.copy(distanceRadius = 0, distanceCenterWorld = "")
                            } else {
                                val loc = click.player.location
                                f.copy(
                                    distanceRadius = 50,
                                    distanceCenterX = loc.x,
                                    distanceCenterZ = loc.z,
                                    distanceCenterWorld = loc.world.name,
                                )
                            }
                        }

                        event.click.isRightClick -> {
                            val newRadius = (f.distanceRadius - 1).coerceAtLeast(if (f.distanceEnabled) 1 else 0)
                            context.filter = f.copy(distanceRadius = newRadius)
                        }

                        else -> {
                            context.filter = f.copy(distanceRadius = f.distanceRadius + 1)
                        }
                    }
                    FiltersInterface.openInventory(click.player, context)
                },
            // Query profiles button
            item()
                .atSlot(8)
                .displayAs(
                    createItem(Material.WRITTEN_BOOK, mm("<white>Query Profiles"), listOf(mm("<gray>Load or manage saved filters."))),
                ).routeTo(ProfileListInterface) { ProfileListContext() },
            // Save as profile button
            item()
                .atSlot(bottomRow + 6)
                .displayAs(
                    createItem(Material.WRITABLE_BOOK, mm("<white>Save as Profile"), listOf(mm("<gray>Save the current filter state."))),
                ).onClick {
                    ChatInputManager.awaitInput(click.player, "<gray>Type a name for this profile:") { name ->
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            FiltersInterface.openInventory(click.player, context)
                            return@awaitInput
                        }
                        when (QueryProfileManager.save(trimmed, context.filter)) {
                            ProfileSaveResult.Saved ->
                                click.player.msg("<green>Profile '<white>$trimmed</white>' saved.")
                            ProfileSaveResult.DuplicateName ->
                                click.player.msg("<red>A profile named '<white>$trimmed</white>' already exists.")
                        }
                        FiltersInterface.openInventory(click.player, context)
                    }
                },
        )
}
