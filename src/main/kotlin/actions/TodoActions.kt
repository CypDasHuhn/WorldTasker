package dev.cypdashuhn.worldtasker.actions

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.query.TodoQuery
import dev.cypdashuhn.worldtasker.commands.query.executeTodoQuery
import dev.cypdashuhn.worldtasker.db.HistoryManager
import dev.cypdashuhn.worldtasker.db.TagAssignResult
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoCreateResult
import dev.cypdashuhn.worldtasker.db.TodoManager
import dev.cypdashuhn.worldtasker.db.TodoState
import dev.cypdashuhn.worldtasker.db.TodoStateResult
import dev.cypdashuhn.worldtasker.db.TodoUpdateNameResult
import dev.rooster.db.utility_tables.LocationManager
import dev.rooster.db.utility_tables.PlayerManager
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

private fun Player.multipleScopeTags() = msg("<red>A todo can only have one scope tag.")

private fun Player.singleTagViolation(namespaces: List<String>) {
    val nsList = namespaces.joinToString(", ")
    msg("<red>Namespace(s) <white>$nsList</white> only allow one tag per todo. Remove the existing tag first.")
}

private fun Player.scopeCollision(todoName: String, scopeTagName: String?) {
    val scopeDesc = if (scopeTagName != null) "scoped to '<white>$scopeTagName</white>'" else "with no scope tag"
    msg("<red>A todo named '<white>$todoName</white>' $scopeDesc already exists.")
}

object TodoActions {
    fun add(sender: Player, name: String, description: String, tagsStr: String?) {
        val tagIds = if (tagsStr != null) resolveTagIds(tagsStr, sender) else emptyList()
        when (val result = TodoManager.create(name, sender, description, sender.location, tagIds)) {
            is TodoCreateResult.Created -> sender.msg("<green>Todo '<white>$name</white>' created.")
            TodoCreateResult.MultipleScopeTags -> sender.multipleScopeTags()
            is TodoCreateResult.ScopeCollision -> sender.scopeCollision(name, result.scopeTagName)
        }
    }

    fun complete(sender: Player, id: Int) {
        when (TodoManager.complete(id, sender)) {
            TodoStateResult.SUCCESS -> sender.msg("<green>Todo marked complete.")
            TodoStateResult.WRONG_STATE -> sender.msg("<red>That todo is not active.")
        }
    }

    fun reactivate(sender: Player, id: Int) {
        when (TodoManager.reactivate(id, sender)) {
            TodoStateResult.SUCCESS -> sender.msg("<green>Todo reactivated.")
            TodoStateResult.WRONG_STATE -> sender.msg("<red>That todo is not completed.")
        }
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
        when (TodoManager.work(id, sender, comment.ifBlank { null })) {
            TodoStateResult.SUCCESS -> sender.msg("<green>Work entry recorded.")
            TodoStateResult.WRONG_STATE -> sender.msg("<red>That todo is not active.")
        }
    }

    fun setTags(sender: Player, id: Int, tagsStr: String) {
        val newTagIds = resolveTagIds(tagsStr, sender)
        when (val result = TagManager.setTagsForTodo(id, newTagIds)) {
            TagAssignResult.Success -> sender.msg("<green>Tags set.")
            TagAssignResult.MultipleScopeTags -> sender.multipleScopeTags()
            is TagAssignResult.ScopeCollision -> sender.scopeCollision(result.todoName, result.scopeTagName)
            is TagAssignResult.NamespaceSingleTagViolation -> sender.singleTagViolation(result.namespaceNames)
        }
    }

    fun addTags(sender: Player, id: Int, tagsStr: String) {
        val newTagIds = resolveTagIds(tagsStr, sender)
        when (val result = TagManager.addTagsToTodo(id, newTagIds)) {
            TagAssignResult.Success -> sender.msg("<green>Tags added.")
            TagAssignResult.MultipleScopeTags -> sender.multipleScopeTags()
            is TagAssignResult.ScopeCollision -> sender.scopeCollision(result.todoName, result.scopeTagName)
            is TagAssignResult.NamespaceSingleTagViolation -> sender.singleTagViolation(result.namespaceNames)
        }
    }

    fun removeTags(sender: Player, id: Int, tagsStr: String) {
        val removeTagIds = resolveTagIds(tagsStr, sender)
        when (val result = TagManager.removeTagsFromTodo(id, removeTagIds)) {
            TagAssignResult.Success -> sender.msg("<green>Tags removed.")
            TagAssignResult.MultipleScopeTags -> sender.multipleScopeTags()
            is TagAssignResult.ScopeCollision -> sender.scopeCollision(result.todoName, result.scopeTagName)
            is TagAssignResult.NamespaceSingleTagViolation -> sender.singleTagViolation(result.namespaceNames)
        }
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
        val countLabel = if (results.size > cap) {
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
            val time = entry[HistoryManager.History.time].format(
                java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm")
            )
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
        val tags = TagManager.tagLabelsForTodo(id)
        if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
        sender.msg("<gray>\"${todo[TodoManager.Todos.description]}\"")
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
        val location = transaction { LocationManager.Location.findById(todo[TodoManager.Todos.locationId]!!)?.location() }
        if (location == null) {
            sender.msg("<red>Location data for '<white>$name</white>' is missing.")
            return
        }
        sender.teleport(location)
        sender.msg("<green>Teleported to random todo '<white>$name</white>'.")
        val tags = TagManager.tagLabelsForTodo(id)
        if (tags.isNotEmpty()) sender.msg("<green>Tags:<gray> ${tags.joinToString(", ")}")
        sender.msg("<gray>\"${todo[TodoManager.Todos.description]}\"")
    }
}
