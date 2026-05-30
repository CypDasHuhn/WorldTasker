package dev.cypdashuhn.worldtasker

import com.google.common.cache.CacheBuilder
import dev.cypdashuhn.rooster.common.RoosterCache
import dev.cypdashuhn.rooster.common.RoosterServices
import dev.cypdashuhn.rooster.common.initRooster
import dev.cypdashuhn.rooster.db.db
import dev.cypdashuhn.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.rooster.db.utility_tables.PlayerManager
import dev.cypdashuhn.rooster.db.utility_tables.attributes.PlayerAttributeManager
import dev.cypdashuhn.rooster.localization.provider.LocaleProvider
import dev.cypdashuhn.rooster.localization.provider.YmlLocaleProvider
import dev.cypdashuhn.worldtasker.commands.todo
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.CommandAPIPaperConfig
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.TimeUnit


class WorldTaskerPlugin : JavaPlugin() {

    companion object {
        lateinit var plugin: JavaPlugin
        val services = RoosterServices()
        val cache = RoosterCache<String, Any>(CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES))
        val playerManager by services.setDelegate(PlayerManager())
        val playerAttributeManager by services.setDelegate(PlayerAttributeManager(playerManager))
        val locationManager by services.setDelegate(LocationManager())
    }

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIPaperConfig(this).verboseOutput(false)) // Load with verbose output
    }

    override fun onEnable() {
        plugin = this


        initRooster(plugin, services, cache) {
            services.setDelegate<LocaleProvider>(
                YmlLocaleProvider(
                    mapOf(
                        "en_US" to Locale.ENGLISH,
                        "de_DE" to Locale.GERMAN
                    ), "en_US"
                )
            )

            db(listOf())
        }

        CommandAPI.onEnable()
        initCommands()
    }

    fun initCommands() {
        todo()
    }
}
