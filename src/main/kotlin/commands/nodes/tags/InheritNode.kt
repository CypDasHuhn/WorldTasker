package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.actions.TagActions
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesCommaText
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val CHILD       = "inheritChild"
private const val ADD_PARENT  = "inheritAddParent"
private const val SET_PARENTS = "inheritSetParents"
private const val REM_PARENT  = "inheritRemoveParent"

internal fun buildInheritNode(): LiteralArgument {
    val inheritNode = LiteralArgument("inherit")
    val childArg = StringArgument(CHILD).suggestTagNames()

    val addParentArg = StringArgument(ADD_PARENT).suggestTagNames()
    addParentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TagActions.addInheritance(sender, args.argsMap[CHILD] as String, args.argsMap[ADD_PARENT] as String)
    })
    childArg.then(LiteralArgument("add").then(addParentArg))

    val setParentsArg = TextArgument(SET_PARENTS).suggestTagNamesCommaText()
    setParentsArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TagActions.setInheritance(sender, args.argsMap[CHILD] as String, args.argsMap[SET_PARENTS] as String)
    })
    childArg.then(LiteralArgument("set").then(setParentsArg))

    val remParentArg = StringArgument(REM_PARENT).suggestTagNames()
    remParentArg.executesPlayer(PlayerCommandExecutor { sender, args ->
        TagActions.removeInheritance(sender, args.argsMap[CHILD] as String, args.argsMap[REM_PARENT] as String)
    })
    childArg.then(LiteralArgument("remove").then(remParentArg))

    inheritNode.then(childArg)
    return inheritNode
}
