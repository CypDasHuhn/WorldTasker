package dev.cypdashuhn.worldtasker.ui

import dev.rooster.core.RoosterCore
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ChatInputManager : Listener {
    private val listeners = ConcurrentHashMap<UUID, (String) -> Unit>()
    private val mm = MiniMessage.miniMessage()

    fun awaitInput(player: Player, prompt: String? = null, callback: (String) -> Unit) {
        listeners[player.uniqueId] = callback
        RoosterCore.plugin.server.scheduler.runTask(RoosterCore.plugin, Runnable {
            player.closeInventory()
            if (prompt != null) player.sendMessage(mm.deserialize(prompt))
        })
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val callback = listeners.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        RoosterCore.plugin.server.scheduler.runTask(RoosterCore.plugin, Runnable {
            callback(message)
        })
    }
}
