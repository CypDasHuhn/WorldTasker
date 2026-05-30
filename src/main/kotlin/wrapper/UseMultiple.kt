package dev.cypdashuhn.worldtasker.commands.wrapper

import dev.jorel.commandapi.AbstractArgumentTree
import dev.jorel.commandapi.AbstractCommandTree
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.executors.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.*
/*
fun ACT.thenMerged(vararg arg: Argument<*>) = MergedTree(this).then(*arg)
fun ACT.thenNestedMerged(vararg arg: ArgumentPart) = MergedTree(this).thenNestedMerged(*arg)

fun test() {
    CommandTree("!test").thenNestedMerged(
        TextArgument("test1").single(),
        MultiArgumentPart.of(IntegerArgument("number"), TextArgument("text")),
        TextArgument("test2").single(),
    ).executes(CommandExecutor { sender, info -> sender.sendMessage("test") }).register()

    CommandTree("test")
        .thenMerged(TextArgument("a"), IntegerArgument("b"))
        .then(TextArgument("c"))
        .then(TextArgument("d"), TextArgument("e"))
        .executes { sender, info -> sender.sendMessage("test") }
        .merge()
        .then(TextArgument("f"))
        .register()

    var s = IntegerArgument("s").then(TextArgument("t"))

    CommandTree("!test2").then(LiteralArgument("test1"))
        .executes(CommandExecutor { sender, info -> sender.sendMessage("test") }).register()
}

interface ArgumentPart {
    fun getArguments(): List<A>
    val isSingle get() = getArguments().size == 1
}

class SingleArgumentPart<Impl : AbstractArgumentTree<Impl, Argument<*>, CommandSender>>(val instance: Impl) :
    AbstractArgumentTree<Impl, Argument<*>, CommandSender>(), ArgumentPart {
    override fun getArguments() = listOf(this)

    override fun instance(): Impl? {
        return instance
    }
}

fun Argument<*>.single() = SingleArgumentPart(this)
class MultiArgumentPart(val arguments: List<AAT>) : ArgumentPart {
    override fun getArguments() = arguments

    companion object {
        fun of(vararg args: Argument<*>) = MultiArgumentPart(args.toList())
    }
}

typealias StackPart = AAT.(AAT.() -> AAT) -> AAT

class MergedTree {
    companion object {
        fun thenMerged(
            tree: AAT,
            vararg args: AAT,
            block: AAT.() -> AAT,
        ): AAT {
            var tree = tree
            val suggestionList: MutableList<ArgumentSuggestions<CommandSender>> = mutableListOf()

            args.forEachIndexed { idx, arg ->
                val isLast = idx == args.size - 1
                val argInstance = arg.argument()
                if (argInstance.overriddenSuggestions.isPresent) suggestionList += argInstance.overriddenSuggestions.get()
                if (!isLast) {
                    findFieldRecursive(arg::class.java, "suggestions").apply {
                        isAccessible = true
                        set(arg, Optional.empty<ArgumentSuggestions<CommandSender>>())
                    }
                }
                if (isLast) tree =
                    tree.then(
                        argInstance.replaceSuggestions(ArgumentSuggestions.merge(*suggestionList.toTypedArray()))
                            .block()
                    )
                tree = tree.then(arg.block())
            }

            return tree
        }

        fun thenNestedMerged(tree: AAT, vararg args: ArgumentPart): AAT {
            var tree = tree
            var argIndex = 0
            while (true) {
                if (argIndex >= args.size) break
                val arg = args[argIndex]

                if (arg.isSingle) tree = tree.then(arg.getArguments().first())
                else return thenMerged(tree, *arg.getArguments().toTypedArray()) {
                    thenNestedMerged(tree, *args.drop(argIndex + 1).toTypedArray())
                }
                argIndex++
            }

            return tree
        }
    }

    constructor(tree: ACT) {
        this.tree = tree
        this.stack = mutableListOf()
    }

    private constructor(
        tree: ACT,
        stack: List<Pair<StackPart, Boolean>>
    ) {
        this.stack = stack.toMutableList()
        this.tree = tree
    }

    val tree: ACT
    val stack: MutableList<Pair<StackPart, Boolean>>
    private fun copy(isNested: Boolean = false, stackAddition: StackPart): MergedTree {
        return MergedTree(tree, stack + (stackAddition to isNested))
    }

    fun then(vararg args: AAT): MergedTree {
        if (args.isEmpty()) throw IllegalArgumentException("At least one argument must be provided")
        return if (args.size == 1) copy { then(args.first()) }
        // TODO: LOOK INTO AAT/ACT
        else copy { thenMerged(tree as AAT, *args, block = it) }
    }

    fun thenNestedMerged(vararg args: ArgumentPart): MergedTree = copy { thenNestedMerged(this, *args) }
    fun thenNested(vararg args: AAT): MergedTree =
        copy { thenNested(*args) }

    fun executes(executor: CommandExecutor): MergedTree = copy {
        argument().executes(executor)
    }

    //TODO: LOOK INTO AAT/ACT
    fun merge() = (tree as AAT).merge(stack) as ACT
    fun register() = merge().register()
}

fun AAT.merge(stack: List<Pair<StackPart, Boolean>>): AAT {
    var tree = this as AAT
    for ((idx, pair) in stack.withIndex()) {
        val (stackPart, isNested) = pair
        val list = stack.drop(idx)
        tree = tree.stackPart { merge(list) }
        if (isNested) break
    }
    return tree
}

internal fun AAT.argument(): Argument<*> {
    findFieldRecursive(this::class.java, "argument").apply {
        isAccessible = true
        return get(this) as Argument<*>
    }
}

typealias ACT = AbstractCommandTree<*, Argument<*>, CommandSender>
typealias AAT = AbstractArgumentTree<*, Argument<*>, CommandSender>
 */
