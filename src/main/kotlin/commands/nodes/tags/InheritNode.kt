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

private const val CHILD = "inheritChild"
private const val ADD_PARENT = "inheritAddParent"
private const val SET_PARENTS = "inheritSetParents"
private const val REM_PARENT = "inheritRemoveParent"

internal fun buildInheritNode(): LiteralArgument =
    la("inherit").apply {
        then(NamespacedKeyArgument(CHILD).suggestTagNames().apply {
            then(la("add").then(
                NamespacedKeyArgument(ADD_PARENT)
                    .suggestTagNames()
                    .executesPlayer(PlayerCommandExecutor { sender, args ->
                        TagActions.addInheritance(sender, args.argsMap[CHILD] as NamespacedKey, args.argsMap[ADD_PARENT] as NamespacedKey)
                    })
            ))
            then(la("set").then(
                GreedyStringArgument(SET_PARENTS)
                    .suggestTagNamesGreedy()
                    .executesPlayer(PlayerCommandExecutor { sender, args ->
                        TagActions.setInheritance(sender, args.argsMap[CHILD] as NamespacedKey, args.argsMap[SET_PARENTS] as String)
                    })
            ))
            then(la("remove").then(
                NamespacedKeyArgument(REM_PARENT)
                    .suggestTagNames()
                    .executesPlayer(PlayerCommandExecutor { sender, args ->
                        TagActions
                            .removeInheritance(sender, args.argsMap[CHILD] as NamespacedKey, args.argsMap[REM_PARENT] as NamespacedKey)
                    })
            ))
        })
    }
