package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

//region node names
private const val ADD_NS      = "todoTagsAddNs"
private const val ADD_NAME    = "todoTagsAddName"
private const val REMOVE_NAME = "todoTagsRemoveName"
private const val RENAME_OLD  = "todoTagsRenameOld"
private const val RENAME_NEW  = "todoTagsRenameNew"
//endregion

internal fun buildTodoTagsNode(): LiteralArgument {
    val tagsNode = LiteralArgument("tags")

    tagsNode.then(
        LiteralArgument("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            val tags = TagManager.all()
            if (tags.isEmpty()) {
                sender.msg("<gray>No tags found.")
                return@PlayerCommandExecutor
            }
            sender.msg("<gold>=== Tags ===")
            tags.forEach { row ->
                val nsId = row[TagManager.Tags.namespaceId].value
                val ns = NamespaceManager.find(nsId)?.get(NamespaceManager.Namespaces.name) ?: "?"
                sender.msg("<yellow>$ns<gray>:<white>${row[TagManager.Tags.name]}")
            }
        })
    )

    val tagsAddNsArg = StringArgument(ADD_NS).suggestNamespaceNames()
    val tagsAddNameArg = StringArgument(ADD_NAME)
    tagsAddNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val nsName = args.argsMap[ADD_NS] as String
        val tagName = args.argsMap[ADD_NAME] as String
        val ns = NamespaceManager.findByName(nsName)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$nsName</white>' not found. Create it first with /todo tags namespaces add.")
            return@PlayerCommandExecutor
        }
        val nsId = ns[NamespaceManager.Namespaces.id].value
        if (TagManager.findByName(tagName, nsId) != null) {
            sender.msg("<red>Tag '<white>$nsName:$tagName</white>' already exists.")
            return@PlayerCommandExecutor
        }
        TagManager.create(tagName, nsId)
        sender.msg("<green>Tag '<white>$nsName:$tagName</white>' created.")
    })
    tagsAddNsArg.then(tagsAddNameArg)
    tagsNode.then(LiteralArgument("add").then(tagsAddNsArg))

    val tagsRemoveNameArg = StringArgument(REMOVE_NAME).suggestTagNames()
    tagsRemoveNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val tagName = args.argsMap[REMOVE_NAME] as String
        val tag = TagManager.findByName(tagName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$tagName</white>' not found.")
            return@PlayerCommandExecutor
        }
        TagManager.delete(tag[TagManager.Tags.id].value)
        sender.msg("<green>Tag '<white>$tagName</white>' removed.")
    })
    tagsNode.then(LiteralArgument("remove").then(tagsRemoveNameArg))

    val tagsRenameOldArg = StringArgument(RENAME_OLD).suggestTagNames()
    val tagsRenameNewArg = StringArgument(RENAME_NEW)
    tagsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val oldName = args.argsMap[RENAME_OLD] as String
        val newName = args.argsMap[RENAME_NEW] as String
        val tag = TagManager.findByName(oldName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$oldName</white>' not found.")
            return@PlayerCommandExecutor
        }
        TagManager.rename(tag[TagManager.Tags.id].value, newName)
        sender.msg("<green>Tag renamed to '<white>$newName</white>'.")
    })
    tagsRenameOldArg.then(tagsRenameNewArg)
    tagsNode.then(LiteralArgument("rename").then(tagsRenameOldArg))

    tagsNode.then(buildInheritNode())
    tagsNode.then(buildNamespacesNode())

    return tagsNode
}
