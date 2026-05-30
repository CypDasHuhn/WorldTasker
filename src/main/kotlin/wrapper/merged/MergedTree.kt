package dev.cypdashuhn.worldtasker.commands.wrapper.merged
/*
import com.mojang.brigadier.arguments.ArgumentType
import dev.cypdashuhn.worldtasker.commands.wrapper.findFieldRecursive
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.Optional
import kotlin.collections.forEach

typealias A = Argument<*>;

fun CommandTree.thenMerged(vararg args: A) = MergedTree(this, args.toList())
fun CommandTree.thenNestedMerged(vararg args: ArgumentPart) {
    val tree = MergedTree(this, args.first().getArguments())
    val rest = args.drop(1)

    return tree.thenNestedMerged(*rest.toTypedArray())
}

class MergedTree {
    var tree: CommandTree
    var arguments: List<A> = emptyList()

    constructor(tree: CommandTree, args: List<A>) {
        this.tree = tree
        this.arguments = transformArguments(args);
    }

    fun then(arg: A): MergedTree {
        val newArgs = arguments.map { it.then(arg) }
        return MergedTree(tree, newArgs)
    }

    fun thenMerged(vararg args: A): MergedTree {
        val newArgs = mutableListOf<A>()
        val transformedArgs = transformArguments(args.toList())
        arguments.forEach { arg ->
            transformedArgs.forEach { arg2 ->
                newArgs.add(arg.then(arg2))
            }
        }
        return MergedTree(tree, newArgs)
    }

    fun thenNested(vararg args: A): MergedTree {
        val newArgs = arguments.map { it.thenNested(*args) }
        return MergedTree(tree, newArgs)
    }

    fun thenNestedMerged(vararg args: ArgumentPart) {
        var newArgs = arguments
        args.forEach { argPart ->
            if (argPart.isSingle) newArgs = newArgs.map { it.then(argPart.getArguments().first()) }
            else {
                val args = argPart.getArguments()
                val localNewArgs = newArgs.toMutableList()
                args.forEach { arg ->
                    localNewArgs.addAll(newArgs.map { it.then(arg) })
                }
                newArgs = localNewArgs
            }
        }
    }

    fun executes(executor: CommandExecutor): MergedTree {
        val newArgs = arguments.map { it.executes(executor) }
        return MergedTree(tree, newArgs)
    }

    fun merge(): CommandTree {
        arguments.forEach { arg -> tree.then(arg) }
        return tree
    }
    fun register() {
        merge().register()
    }
}

val typeMap = mapOf<ArgumentType<*>, Int>(
)
fun transformArguments(args: List<A>): List<A> {
    val sorted = args.sortedBy { typeMap.getOrDefault(it.rawType, Int.MAX_VALUE) }

    val suggestions = mutableListOf<ArgumentSuggestions<CommandSender>>()
    sorted.withIndex().forEach { (idx, arg) ->
        suggestions.add(arg.overriddenSuggestions.get())

        val isLast = idx == sorted.size - 1
        if (!isLast) {
            findFieldRecursive(arg::class.java, "suggestions").apply {
                isAccessible = true
                set(arg, Optional.empty<ArgumentSuggestions<CommandSender>>())
            }
        } else {
            arg.replaceSuggestions(ArgumentSuggestions.merge(*suggestions.toTypedArray()))
        }
    }

    return sorted
}

interface ArgumentPart {
    fun getArguments(): List<A>
    val isSingle get() = getArguments().size == 1
}

class SingleArgumentPart(val arg: Argument<*>) : ArgumentPart {
    override fun getArguments() = listOf<A>(arg)
}

fun Argument<*>.single() = SingleArgumentPart(this)
class MultiArgumentPart(val arguments: List<A>) : ArgumentPart {
    override fun getArguments() = arguments

    companion object {
        fun of(vararg args: Argument<*>) = MultiArgumentPart(args.toList())
    }
}


fun syntaxExample() {
    var tree = CommandTree("test")
        .thenMerged(
    TextArgument("branch1 a"),
            IntegerArgument("branch2 b")
        )
        .then(TextArgument("node1"))
        .thenMerged(IntegerArgument("branch2 a"), TextArgument("branch2 b"))
        .then(TextArgument("node2"))
        .executes { sender, info -> sender.sendMessage("test") }
        .merge()
        .register()


    /* * Tree should look like this:
    /test [branch1 a / branch1 b] [node1] [branch2 a / branch2 b] [node2]
    /test [branch1 a] [node1] [branch2 a] [node2]
    /test [branch1 b] [node1] [branch2 a] [node2]
    /test [branch1 a] [node1] [branch2 b] [node2]
    /test [branch1 b] [node1] [branch2 b] [node2]
    */
}

 */
