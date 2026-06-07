package dev.cypdashuhn.worldtasker.commands.nodes.tags

import dev.cypdashuhn.worldtasker.actions.TagActions
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.suggestTagNames
import dev.cypdashuhn.worldtasker.commands.suggestTagNamesGreedy
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.NamespacedKeyArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

private const val CHILD = "inheritChild"
private const val ADD_PARENT = "inheritAddParent"
private const val SET_PARENTS = "inheritSetParents"
private const val REM_PARENT = "inheritRemoveParent"

private fun inheritBranch(literal: String, argKey: String, action: (Player, NamespacedKey, String) -> Unit) =
    la(literal).then(
        GreedyStringArgument(argKey)
            .suggestTagNamesGreedy()
            .executesPlayer(PlayerCommandExecutor { sender, args ->
                action(sender, args.argsMap[CHILD] as NamespacedKey, args.argsMap[argKey] as String)
            })
    )

internal fun buildInheritNode(): LiteralArgument =
    la("inherit").apply {
        then(NamespacedKeyArgument(CHILD).suggestTagNames().apply {
            then(inheritBranch("add", ADD_PARENT, TagActions::addInheritance))
            then(inheritBranch("set", SET_PARENTS, TagActions::setInheritance))
            then(inheritBranch("remove", REM_PARENT, TagActions::removeInheritance))
        })
    }
