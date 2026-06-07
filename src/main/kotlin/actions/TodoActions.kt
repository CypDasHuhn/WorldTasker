package dev.cypdashuhn.worldtasker.actions

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.query.TodoQuery
import dev.cypdashuhn.worldtasker.commands.query.executeTodoQuery
import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoScopeManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.cypdashuhn.worldtasker.db.TodoStatus
import dev.rooster.db.utility_tables.LocationManager
import dev.rooster.db.utility_tables.PlayerManager
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

private fun Player.multipleScopeTags() = msg("<red>A todo can only have one scope tag.")

private fun Player.scopeCollision(todoName: String, scopeTagName: String?) {
    val scopeDesc = if (scopeTagName != null) "scoped to '<white>$scopeTagName</white>'" else "with no scope tag"
    msg("<red>A todo named '<white>$todoName</white>' $scopeDesc already exists.")
}

object TodoActions {
    fun add(sender: Player, name: String, description: String, tagsStr: String?) {
        val tagIds = if (tagsStr != null) resolveTagIds(tagsStr, sender) else emptyList()
        if (TodoScopeManager.countScopeTagsAmong(tagIds) > 1) { sender.multipleScopeTags(); return }
        val scopeTagName = TodoScopeManager.scopeTagNameAmong(tagIds)
        if (TodoScopeManager.wouldCollide(name, scopeTagName)) {
            sender.scopeCollision(name, scopeTagName)
            return
        }
        val id = TodoManager.create(name, sender, description, sender.location)
        tagIds.forEach { TagManager.addToTodo(id, it) }
        sender.msg("<green>Todo '<white>$name</white>' created.")
    }

    fun complete(sender: Player, id: Int) {
        if (TodoManager.stateOf(id) != TodoState.ACTIVE) {
            sender.msg("<red>That todo is not active.")
            return
        }
        TodoManager.complete(id, sender)
        sender.msg("<green>Todo marked complete.")
    }

    fun reactivate(sender: Player, id: Int) {
        if (TodoManager.stateOf(id) != TodoState.COMPLETED) {
            sender.msg("<red>That todo is not completed.")
            return
        }
        TodoManager.reactivate(id, sender)
        sender.msg("<green>Todo reactivated.")
    }

    fun updateDescription(sender: Player, id: Int, description: String) {
        TodoManager.updateDescription(id, description)
        sender.msg("<green>Description updated.")
    }

    fun delete(sender: Player, name: String, id: Int) {
        TodoManager.delete(id, sender)
        sender.msg("<green>Todo '<white>$name</white>' removed.")
    }

    fun work(sender: Player, id: Int, comment: String) {
        if (TodoManager.stateOf(id) != TodoState.ACTIVE) {
            sender.msg("<red>That todo is not active.")
            return
        }
        HistoryManager.record(id, sender, TodoStatus.WORK, comment)
        sender.msg("<green>Work entry recorded.")
    }

    fun setTags(sender: Player, id: Int, tagsStr: String) {
        val newTagIds = resolveTagIds(tagsStr, sender)
        if (TodoScopeManager.countScopeTagsAmong(newTagIds) > 1) { sender.multipleScopeTags(); return }
        val todoName = TodoManager.findById(id)?.name ?: return
        val scopeTagName = TodoScopeManager.scopeTagNameAmong(newTagIds)
        if (TodoScopeManager.wouldCollide(todoName, scopeTagName, excludeTodoId = id)) {
            sender.scopeCollision(todoName, scopeTagName)
            return
        }
        TagManager.removeAllForTodo(id)
        newTagIds.forEach { TagManager.addToTodo(id, it) }
        sender.msg("<green>Tags set.")
    }

    fun addTags(sender: Player, id: Int, tagsStr: String) {
        val newTagIds = resolveTagIds(tagsStr, sender)
        val todoName = TodoManager.findById(id)?.name ?: return
        val existingTagIds = TagManager.tagsForTodo(id).map { it[TagManager.Tags.id].value }
        if (TodoScopeManager.countScopeTagsAmong(existingTagIds + newTagIds) > 1) { sender.multipleScopeTags(); return }
        val scopeTagName = TodoScopeManager.scopeTagNameAmong(existingTagIds + newTagIds)
        if (TodoScopeManager.wouldCollide(todoName, scopeTagName, excludeTodoId = id)) {
            sender.scopeCollision(todoName, scopeTagName)
            return
        }
        newTagIds.forEach { TagManager.addToTodo(id, it) }
        sender.msg("<green>Tags added.")
    }

    fun removeTags(sender: Player, id: Int, tagsStr: String) {
        val removeTagIds = resolveTagIds(tagsStr, sender)
        val todoName = TodoManager.findById(id)?.name ?: return
        val existingTagIds = TagManager.tagsForTodo(id).map { it[TagManager.Tags.id].value }
        val scopeTagName = TodoScopeManager.scopeTagNameAmong(existingTagIds - removeTagIds.toSet())
        if (TodoScopeManager.wouldCollide(todoName, scopeTagName, excludeTodoId = id)) {
            sender.scopeCollision(todoName, scopeTagName)
            return
        }
        removeTagIds.forEach { TagManager.removeFromTodo(id, it) }
        sender.msg("<green>Tags removed.")
    }

    fun get(sender: Player, query: TodoQuery, random: Boolean = false) {
        val results = executeTodoQuery(query, sender.location)
        if (results.isEmpty()) {
            sender.msg("<gray>No todos found.")
            return
        }
        if (random) {
            val row = results.random()
            val id = row[TodoManager.Todos.id].value
            val name = row[TodoManager.Todos.name]
            val state = TodoManager.stateOf(id)
            val tags = TagManager.tagLabelsForTodo(id)
            val stateSuffix = if (state == TodoState.COMPLETED) " <green>[✓]" else ""
            val tagSuffix = if (tags.isNotEmpty()) " <dark_gray>| <gray>${tags.joinToString(", ")}" else ""
            sender.msg("<gold>Random todo: <yellow>#$id <white>$name$stateSuffix$tagSuffix")
            return
        }
        val cap = 10
        val shown = results.take(cap)
        val countLabel = if (results.size >
            cap) {
            "<yellow>${results.size}</yellow> <gold>found, showing first $cap"
        } else {
            "<yellow>${results.size}</yellow> <gold>found"
        }
        sender.msg("<gold>=== Todos $countLabel <gold>===")
        shown.forEach { row ->
            val id = row[TodoManager.Todos.id].value
            val name = row[TodoManager.Todos.name]
            val state = TodoManager.stateOf(id)
            val tags = TagManager.tagLabelsForTodo(id)
            val stateSuffix = if (state == TodoState.COMPLETED) " <green>[✓]" else ""
            val tagSuffix = if (tags.isNotEmpty()) " <dark_gray>| <gray>${tags.joinToString(", ")}" else ""
            sender.msg("<yellow>#$id <white>$name$stateSuffix$tagSuffix")
        }
    }

    fun info(sender: Player, name: String, id: Int) {
        val todo = TodoManager.findByName(name) ?: return
        val author = todo[TodoManager.Todos.author]
        val description = todo[TodoManager.Todos.description]
        val tags = TagManager.tagLabelsForTodo(id)
        val inherited = TagManager.inheritedTagLabelsForTodo(id)
        val history = HistoryManager.forTodo(id)
        sender.msg("<gold>=== <white>$name <gold>===")
        sender.msg("<gray>Author: <white>$author")
        sender.msg("<gray>\"<white>$description<gray>\"")
        if (tags.isNotEmpty()) sender.msg("<gray>Tags: <white>${tags.joinToString(", ")}")
        if (inherited.isNotEmpty()) sender.msg("<gray>Inherited: <dark_gray>${inherited.joinToString(", ")}")
        sender.msg("<gold>--- History ---")
        history.forEach { entry ->
            val time = entry[HistoryManager.History.time].format(java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm"))
            val entryAuthor = entry[PlayerManager.Players.name]
            val status = entry[HistoryManager.History.status]
            val comment = entry[HistoryManager.History.comment]
            val commentPart = if (comment != null) " <gray>— <white>$comment" else ""
            sender.msg("<dark_gray>$time <yellow>$status <gray>($entryAuthor)$commentPart")
        }
    }

    fun jump(sender: Player, name: String, id: Int) {
        val todo = TodoManager.findByName(name) ?: return
        val locId = todo[TodoManager.Todos.locationId]
        if (locId == null) {
            sender.msg("<red>Todo '<white>$name</white>' has no location.")
            return
        }
        val location = transaction { LocationManager.Location.findById(locId)?.location() }
        if (location == null) {
            sender.msg("<red>Location data for '<white>$name</white>' is missing.")
            return
        }
        sender.teleport(location)
        sender.msg("<green>Teleported to todo '<white>$name</white>'.")
        val description = todo[TodoManager.Todos.description]
        val tags = TagManager.tagLabelsForTodo(id)
        if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
        sender.msg("<gray>\"$description\"")
    }

    fun jumpRandom(sender: Player) {
        val candidates = transaction { TodoManager.Todos.selectAll().toList() }
            .filter { row ->
                row[TodoManager.Todos.locationId] != null &&
                    TodoManager.stateOf(row[TodoManager.Todos.id].value) == TodoState.ACTIVE
            }
        val todo = candidates.randomOrNull()
        if (todo == null) {
            sender.msg("<red>No active todos with a location found.")
            return
        }
        val id = todo[TodoManager.Todos.id].value
        val name = todo[TodoManager.Todos.name]
        val locId = todo[TodoManager.Todos.locationId]!!
        val location = transaction { LocationManager.Location.findById(locId)?.location() }
        if (location == null) {
            sender.msg("<red>Location data for '<white>$name</white>' is missing.")
            return
        }
        sender.teleport(location)
        sender.msg("<green>Teleported to random todo '<white>$name</white>'.")
        val description = todo[TodoManager.Todos.description]
        val tags = TagManager.tagLabelsForTodo(id)
        if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
        sender.msg("<gray>\"$description\"")
    }
}
