package dev.cypdashuhn.worldtasker

import dev.cypdashuhn.worldtasker.commands.initCommands
import dev.rooster.core.RoosterServices
import dev.rooster.core.initRooster
import dev.rooster.db.db
import dev.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.worldtasker.commands.todo
import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.initDb
import dev.cypdashuhn.worldtasker.ui.TodoDetailInterface
import dev.cypdashuhn.worldtasker.ui.TodoListInterface
import dev.cypdashuhn.worldtasker.ui.initUi
import dev.rooster.ui.ui
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIPaperConfig
import dev.rooster.localization.provider.YmlLocaleProvider.Companion.addYmlLocaleProvider
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


class WorldTaskerPlugin : JavaPlugin() {

    companion object {
        lateinit var plugin: JavaPlugin
        val services = RoosterServices()
        val locationManager by services.setDelegate(LocationManager())
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIPaperConfig(this).verboseOutput(false)) // Load with verbose output
    }

    override fun onEnable() {
        plugin = this


        initRooster(plugin, services) {
            services.addYmlLocaleProvider(
                mapOf(
                    "en_US" to Locale.ENGLISH,
                    "de_DE" to Locale.GERMAN
                ), "en_US"
            )

            initDb()
            initUi()
        }

        CommandAPI.onEnable()
        initCommands()
    }
}
