package dev.cypdashuhn.worldtasker

import dev.cypdashuhn.worldtasker.commands.initCommands
import dev.cypdashuhn.worldtasker.commands.todo
import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.db.initDb
import dev.cypdashuhn.worldtasker.ui.ChatInputManager
import dev.cypdashuhn.worldtasker.ui.initUi
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIPaperConfig
import dev.rooster.core.RoosterServices
import dev.rooster.core.initRooster
import dev.rooster.db.db
import dev.rooster.db.utility_tables.LocationManager
import dev.rooster.db.utility_tables.PlayerManager
import dev.rooster.localization.provider.YmlLocaleProvider
import dev.rooster.ui.ui
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class WorldTaskerPlugin : JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
        val services = RoosterServices()
        val locationManager by services.setDelegate(LocationManager())
        val playerManager by services.setDelegate(PlayerManager())
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIPaperConfig(this).verboseOutput(false)) // Load with verbose output
    }

    override fun onEnable() {
        plugin = this

        initRooster(plugin, services) {
            services.set(YmlLocaleProvider(
                mapOf(
                    "en_US" to Locale.ENGLISH,
                    "de_DE" to Locale.GERMAN
                ), "en_US"
            ))

            initDb()
            initUi()
        }

        TodoScopeManager.load()
        CommandAPI.onEnable()
        initCommands()
        Bukkit.getPluginManager().registerEvents(ChatInputManager, this)
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun onJoin(event: PlayerJoinEvent) = playerManager.playerLogin(event.player)
        }, this)
        Bukkit.getOnlinePlayers().forEach { playerManager.playerLogin(it) }
    }
}
