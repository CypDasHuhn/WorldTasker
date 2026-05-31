package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.commands.msg
import dev.cypdashuhn.worldtasker.commands.resolveTagIds
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesCommaText
import dev.cypdashuhn.worldtasker.db.TagManager
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

//region node names
private const val CHILD        = "inheritChild"
private const val ADD_PARENT   = "inheritAddParent"
private const val SET_PARENTS  = "inheritSetParents"
private const val REM_PARENT   = "inheritRemoveParent"
//endregion

internal fun buildInheritNode(): LiteralArgument {
    val inheritNode = LiteralArgument("inherit")
    val childArg = StringArgument(CHILD).suggestTagNames()

    val addParentArg = StringArgument(ADD_PARENT).suggestTagNames()
    addParentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val childName = args.argsMap[CHILD] as String
        val parentName = args.argsMap[ADD_PARENT] as String
        val child = TagManager.findByName(childName)
        val parent = TagManager.findByName(parentName)
        if (child == null) { sender.msg("<red>Tag '<white>$childName</white>' not found."); return@PlayerCommandExecutor }
        if (parent == null) { sender.msg("<red>Tag '<white>$parentName</white>' not found."); return@PlayerCommandExecutor }
        TagManager.addInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>$childName</white>' now inherits from '<white>$parentName</white>'.")
    })
    childArg.then(LiteralArgument("add").then(addParentArg))

    val setParentsArg = TextArgument(SET_PARENTS).suggestTagNamesCommaText()
    setParentsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val childName = args.argsMap[CHILD] as String
        val child = TagManager.findByName(childName)
        if (child == null) { sender.msg("<red>Tag '<white>$childName</white>' not found."); return@PlayerCommandExecutor }
        val parentIds = resolveTagIds(args.argsMap[SET_PARENTS] as String, sender)
        TagManager.setInheritance(child[TagManager.Tags.id].value, parentIds)
        sender.msg("<green>Inheritance for '<white>$childName</white>' updated.")
    })
    childArg.then(LiteralArgument("set").then(setParentsArg))

    val remParentArg = StringArgument(REM_PARENT).suggestTagNames()
    remParentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        val childName = args.argsMap[CHILD] as String
        val parentName = args.argsMap[REM_PARENT] as String
        val child = TagManager.findByName(childName)
        val parent = TagManager.findByName(parentName)
        if (child == null) { sender.msg("<red>Tag '<white>$childName</white>' not found."); return@PlayerCommandExecutor }
        if (parent == null) { sender.msg("<red>Tag '<white>$parentName</white>' not found."); return@PlayerCommandExecutor }
        TagManager.removeInheritance(child[TagManager.Tags.id].value, parent[TagManager.Tags.id].value)
        sender.msg("<green>'<white>$childName</white>' no longer inherits from '<white>$parentName</white>'.")
    })
    childArg.then(LiteralArgument("remove").then(remParentArg))

    inheritNode.then(childArg)
    return inheritNode
}
