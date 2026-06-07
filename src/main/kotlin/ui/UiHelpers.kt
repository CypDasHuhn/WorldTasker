package dev.cypdashuhn.worldtasker.ui

import dev.rooster.ui.interfaces.Context
import dev.rooster.ui.interfaces.constructors.confirmation.CancelInfo
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

private val miniMessage: MiniMessage = MiniMessage.miniMessage()

internal fun mm(s: String) = miniMessage.deserialize(s) as TextComponent

internal fun <T : Context> CancelInfo<T>.player(): Player? =
    cancelEvent.clickInfo?.second?.player
        ?: cancelEvent.closeInfo?.player as? Player
