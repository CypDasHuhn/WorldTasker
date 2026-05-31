package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.resolveTagIds
import dev.cypdashuhn.worldtasker.commands.suggestNamespaceNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesCommaText
import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

//region node names
private const val TAGS_ADD_NS = "todoTagsAddNs"
private const val TAGS_ADD_NAME = "todoTagsAddName"
private const val TAGS_REMOVE_NAME = "todoTagsRemoveName"
private const val TAGS_RENAME_OLD = "todoTagsRenameOld"
private const val TAGS_RENAME_NEW = "todoTagsRenameNew"
private const val NS_ADD_NAME = "todoNsAddName"
private const val NS_REMOVE_NAME = "todoNsRemoveName"
private const val NS_RENAME_OLD = "todoNsRenameOld"
private const val NS_RENAME_NEW = "todoNsRenameNew"
private const val INHERIT_ADD_CHILD = "inheritAddChild"
private const val INHERIT_ADD_PARENT = "inheritAddParent"
private const val INHERIT_SET_CHILD = "inheritSetChild"
private const val INHERIT_SET_PARENTS = "inheritSetParents"
private const val INHERIT_REM_CHILD = "inheritRemoveChild"
private const val INHERIT_REM_PARENT = "inheritRemoveParent"
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

    val tagsAddNsArg = StringArgument(TAGS_ADD_NS).suggestNamespaceNames()
    val tagsAddNameArg = StringArgument(TAGS_ADD_NAME)
    tagsAddNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val nsName = args.argsMap[TAGS_ADD_NS] as String
        val tagName = args.argsMap[TAGS_ADD_NAME] as String
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

    val tagsRemoveNameArg = StringArgument(TAGS_REMOVE_NAME).suggestTagNames()
    tagsRemoveNameArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val tagName = args.argsMap[TAGS_REMOVE_NAME] as String
        val tag = TagManager.findByName(tagName)
        if (tag == null) {
            sender.msg("<red>Tag '<white>$tagName</white>' not found.")
            return@PlayerCommandExecutor
        }
        TagManager.delete(tag[TagManager.Tags.id].value)
        sender.msg("<green>Tag '<white>$tagName</white>' removed.")
    })
    tagsNode.then(LiteralArgument("remove").then(tagsRemoveNameArg))

    val tagsRenameOldArg = StringArgument(TAGS_RENAME_OLD).suggestTagNames()
    val tagsRenameNewArg = StringArgument(TAGS_RENAME_NEW)
    tagsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val oldName = args.argsMap[TAGS_RENAME_OLD] as String
        val newName = args.argsMap[TAGS_RENAME_NEW] as String
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

private fun buildNamespacesNode(): LiteralArgument {
    val nsNode = LiteralArgument("namespaces")

    nsNode.then(
        LiteralArgument("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            val namespaces = NamespaceManager.all()
            if (namespaces.isEmpty()) {
                sender.msg("<gray>No namespaces found.")
                return@PlayerCommandExecutor
            }
            sender.msg("<gold>=== Namespaces ===")
            namespaces.forEach { sender.msg("<white>${it[NamespaceManager.Namespaces.name]}") }
        })
    )

    val nsAddArg = StringArgument(NS_ADD_NAME)
    nsAddArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NS_ADD_NAME] as String
        NamespaceManager.create(name)
        sender.msg("<green>Namespace '<white>$name</white>' created.")
    })
    nsNode.then(LiteralArgument("add").then(nsAddArg))

    val nsRemoveArg = StringArgument(NS_REMOVE_NAME).suggestNamespaceNames()
    nsRemoveArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val name = args.argsMap[NS_REMOVE_NAME] as String
        val ns = NamespaceManager.findByName(name)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$name</white>' not found.")
            return@PlayerCommandExecutor
        }
        NamespaceManager.delete(ns[NamespaceManager.Namespaces.id].value)
        sender.msg("<green>Namespace '<white>$name</white>' removed.")
    })
    nsNode.then(LiteralArgument("remove").then(nsRemoveArg))

    val nsRenameOldArg = StringArgument(NS_RENAME_OLD).suggestNamespaceNames()
    val nsRenameNewArg = StringArgument(NS_RENAME_NEW)
    nsRenameNewArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val oldName = args.argsMap[NS_RENAME_OLD] as String
        val newName = args.argsMap[NS_RENAME_NEW] as String
        val ns = NamespaceManager.findByName(oldName)
        if (ns == null) {
            sender.msg("<red>Namespace '<white>$oldName</white>' not found.")
            return@PlayerCommandExecutor
        }
        NamespaceManager.rename(ns[NamespaceManager.Namespaces.id].value, newName)
        sender.msg("<green>Namespace renamed to '<white>$newName</white>'.")
    })
    nsRenameOldArg.then(nsRenameNewArg)
    nsNode.then(LiteralArgument("rename").then(nsRenameOldArg))

    return nsNode
}

private fun buildInheritNode(): LiteralArgument {
    val inheritNode = LiteralArgument("inherit")

    val addChildArg = StringArgument(INHERIT_ADD_CHILD).suggestTagNames()
    val addParentArg = StringArgument(INHERIT_ADD_PARENT).suggestTagNames()
    addParentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val childName = args.argsMap[INHERIT_ADD_CHILD] as String
        val parentName = args.argsMap[INHERIT_ADD_PARENT] as String
        val child = TagManager.findByName(childName)
        val parent = TagManager.findByName(parentName)
        if (child == null) {
            sender.msg("<red>Tag '<white>$childName</white>' not found."); return@PlayerCommandExecutor
        }
        if (parent == null) {
            sender.msg("<red>Tag '<white>$parentName</white>' not found."); return@PlayerCommandExecutor
        }
        TagManager.addInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>$childName</white>' now inherits from '<white>$parentName</white>'.")
    })
    addChildArg.then(addParentArg)
    inheritNode.then(LiteralArgument("add").then(addChildArg))

    val setChildArg = StringArgument(INHERIT_SET_CHILD).suggestTagNames()
    val setParentsArg = TextArgument(INHERIT_SET_PARENTS).suggestTagNamesCommaText()
    setParentsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val childName = args.argsMap[INHERIT_SET_CHILD] as String
        val child = TagManager.findByName(childName)
        if (child == null) {
            sender.msg("<red>Tag '<white>$childName</white>' not found."); return@PlayerCommandExecutor
        }
        val parentIds = resolveTagIds(args.argsMap[INHERIT_SET_PARENTS] as String, sender)
        TagManager.setInheritance(child[TagManager.Tags.id].value, parentIds)
        sender.msg("<green>Inheritance for '<white>$childName</white>' updated.")
    })
    setChildArg.then(setParentsArg)
    inheritNode.then(LiteralArgument("set").then(setChildArg))

    val removeChildArg = StringArgument(INHERIT_REM_CHILD).suggestTagNames()
    val removeParentArg = StringArgument(INHERIT_REM_PARENT).suggestTagNames()
    removeParentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val childName = args.argsMap[INHERIT_REM_CHILD] as String
        val parentName = args.argsMap[INHERIT_REM_PARENT] as String
        val child = TagManager.findByName(childName)
        val parent = TagManager.findByName(parentName)
        if (child == null) {
            sender.msg("<red>Tag '<white>$childName</white>' not found."); return@PlayerCommandExecutor
        }
        if (parent == null) {
            sender.msg("<red>Tag '<white>$parentName</white>' not found."); return@PlayerCommandExecutor
        }
        TagManager.removeInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>$childName</white>' no longer inherits from '<white>$parentName</white>'.")
    })
    removeChildArg.then(removeParentArg)
    inheritNode.then(LiteralArgument("remove").then(removeChildArg))

    return inheritNode
}
