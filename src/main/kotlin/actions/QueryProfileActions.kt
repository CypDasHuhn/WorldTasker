package dev.cypdashuhn.worldtasker.actions

import com.google.gson.Gson
import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.db.QueryProfileManager
import dev.cypdashuhn.worldtasker.db.ProfileSaveResult
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.ui.todo.TodoListContext
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import org.bukkit.entity.Player

object QueryProfileActions {
    private val gson = Gson()

    fun list(sender: Player) {
        val profiles = QueryProfileManager.all()
        if (profiles.isEmpty()) {
            sender.msg("<gray>No query profiles saved.")
            return
        }
        sender.msg("<gold>=== Query Profiles ===")
        profiles.forEach { profile ->
            val parts = mutableListOf<String>()
            if (profile.filter.included.isNotEmpty()) parts.add("<green>+${profile.filter.included.size}")
            if (profile.filter.excluded.isNotEmpty()) parts.add("<red>-${profile.filter.excluded.size}")
            if (profile.filter.statusFilter != dev.cypdashuhn.worldtasker.db.StatusFilter.DEFAULT) parts.add("<yellow>status")
            if (profile.filter.authorIncluded.isNotEmpty() || profile.filter.authorExcluded.isNotEmpty()) parts.add("<aqua>author")
            if (profile.filter.distanceEnabled) parts.add("<light_purple>dist")
            val summary = if (parts.isEmpty()) "<gray>empty" else parts.joinToString(" ")
            sender.msg("<white>${profile.name} <dark_gray>· $summary")
        }
    }

    fun save(sender: Player, name: String, filter: TodoFilter) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            sender.msg("<red>Profile name cannot be empty.")
            return
        }
        when (QueryProfileManager.save(trimmed, filter)) {
            ProfileSaveResult.Saved -> sender.msg("<green>Query profile '<white>$trimmed</white>' saved.")
            ProfileSaveResult.DuplicateName -> sender.msg("<red>A profile named '<white>$trimmed</white>' already exists.")
        }
    }

    fun delete(sender: Player, name: String) {
        val profile = QueryProfileManager.findByName(name)
        if (profile == null) {
            sender.msg("<red>Profile '<white>$name</white>' not found.")
            return
        }
        QueryProfileManager.delete(profile[QueryProfileManager.QueryProfiles.id].value)
        sender.msg("<green>Profile '<white>$name</white>' deleted.")
    }

    fun apply(sender: Player, name: String) {
        val row = QueryProfileManager.findByName(name)
        if (row == null) {
            sender.msg("<red>Profile '<white>$name</white>' not found.")
            return
        }
        val filter = gson.fromJson(row[QueryProfileManager.QueryProfiles.filterJson], TodoFilter::class.java)
        TodoListInterface.openInventory(sender, TodoListContext(filter))
    }
}
