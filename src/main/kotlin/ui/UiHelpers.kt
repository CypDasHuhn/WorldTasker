package dev.cypdashuhn.worldtasker.ui

import dev.rooster.core.util.createItem
import dev.rooster.ui.interfaces.ClickInfo
import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.RoosterInterface
import dev.rooster.ui.interfaces.constructors.confirmation.CancelInfo
import dev.rooster.ui.items.InterfaceItem
import dev.rooster.ui.items.Slots
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player

private val miniMessage: MiniMessage = MiniMessage.miniMessage()

internal fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

internal fun <T : Context> CancelInfo<T>.player(): Player? =
    cancelEvent.clickInfo?.second?.player
        ?: cancelEvent.closeInfo?.player as? Player

// ─── interface item helpers ───────────────────────────────────────────────────

fun <C : Context> RoosterInterface<C>.backItemBase(): InterfaceItem<C> =
    item()
        .displayAs(createItem(Material.FEATHER, mm("<white>Back")))

fun <C : Context, E : Context> RoosterInterface<C>.backItem(target: RoosterInterface<E>): InterfaceItem<C> =
    backItemBase()
        .atSlot(bottomRow)
        .routeTo(target)

fun <C : Context, E : Context> RoosterInterface<C>.backItem(
    target: RoosterInterface<E>,
    getContext: ClickInfo<C>.() -> E,
): InterfaceItem<C> =
    backItemBase()
        .atSlot(bottomRow)
        .routeTo(target, getContext)

fun <C : Context> RoosterInterface<C>.backgroundPane(): InterfaceItem<C> =
    item()
        .atSlots(Slots(bottomRow..bottomRow + 8))
        .displayAs(createItem(Material.GRAY_STAINED_GLASS_PANE, mm("")))
        .priority(-999)

fun <C : Context, E : Context> RoosterInterface<C>.backAndBackground(target: RoosterInterface<E>): List<InterfaceItem<C>> =
    listOf(backItem(target), backgroundPane())

fun <C : Context, E : Context> RoosterInterface<C>.backAndBackground(
    target: RoosterInterface<E>,
    getContext: ClickInfo<C>.() -> E,
): List<InterfaceItem<C>> =
    listOf(backItem(target, getContext), backgroundPane())
