package dev.cypdashuhn.worldtasker.commands.wrapper

import dev.jorel.commandapi.IStringTooltip
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.TextArgument
import org.bukkit.command.CommandSender

fun <T> listWithoutSuggestionsArgument(
    key: String,
    listProvider: (CommandSender) -> List<T>,
    entityName: T.() -> String
): Argument<T> {
    return CustomArgument<T, String>(TextArgument(key)) { info ->
        val res = listProvider(info.sender).firstOrNull { it.entityName() == info.input }
        if (res == null) throw error("Invalid name: ", true)
        res
    }
}

fun <T> listArgument(
    key: String,
    listProvider: (CommandSender) -> List<T>,
    entityName: T.() -> String
) = listWithoutSuggestionsArgument(key, listProvider, entityName).replaceSuggestions(
    ArgumentSuggestions.strings { listProvider(it.sender).map { it.entityName() }.toTypedArray() }
)

fun <T> listArgument(
    key: String,
    listProvider: (CommandSender) -> List<T>,
    entityName: T.() -> String,
    suggestion: T.() -> IStringTooltip
): Argument<T> = listWithoutSuggestionsArgument(key, listProvider, entityName).replaceSuggestions(
    ArgumentSuggestions.stringsWithTooltips {
        listProvider(it.sender).map { it.suggestion() }.toTypedArray()
    }
)

fun stringListArgument(
    key: String,
    listProvider: (CommandSender) -> List<String>
): Argument<String> = listArgument(key, listProvider) { this }
