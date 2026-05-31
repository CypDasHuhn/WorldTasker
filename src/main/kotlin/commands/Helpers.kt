package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.cypdashuhn.worldtasker.db.TodoManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

private val mm = MiniMessage.miniMessage()
internal fun Player.msg(text: String) = sendMessage(mm.deserialize(text))

internal fun handleWithTodo(sender: Player, name: String, block: (Int) -> Unit) {
    val todo = TodoManager.findByName(name)
    if (todo == null) {
        sender.msg("<red>Todo '<white>$name</white>' not found.")
        return
    }
    block(todo[TodoManager.Todos.id].value)
}

internal fun resolveTagIds(tagsStr: String, sender: Player): List<Int> {
    return tagsStr.split(Regex("[,\\s]+")).mapNotNull { raw ->
        val name = raw.trim()
        if (name.isEmpty()) return@mapNotNull null
        val tag = TagManager.findByName(name)
        if (tag == null) {
            sender.msg("<yellow>Tag '<white>$name</white>' not found, skipping.")
            null
        } else {
            tag[TagManager.Tags.id].value
        }
    }
}
