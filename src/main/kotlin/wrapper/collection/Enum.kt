package dev.cypdashuhn.worldtasker.commands.wrapper.collection

import dev.cypdashuhn.worldtasker.commands.wrapper.error
import dev.cypdashuhn.worldtasker.commands.wrapper.transform
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import org.bukkit.command.CommandSender

inline fun <reified T : Enum<T>> toEnum(ignoreCase: Boolean = true): CustomArgument.CustomArgumentInfoParser<T, String> {
    val enumValues = enumValues<T>()
    enumValues.associateBy { it.name.lowercase() }

    return CustomArgument.CustomArgumentInfoParser { input ->
        val entry = enumValues.firstOrNull { it.name.equals(input.input, ignoreCase = ignoreCase) }
        if (entry == null) throw error("Entry not matching")
        entry
    }
}

inline fun <reified T : Enum<T>> enumSuggestions(): ArgumentSuggestions<CommandSender> {
    val enumValues = enumValues<T>()
    return ArgumentSuggestions.strings { enumValues.map { it.name }.toTypedArray() }
}

inline fun <reified T : Enum<T>> Argument<String>.transformToEnum() =
    transform(toEnum<T>()).replaceSuggestions(enumSuggestions<T>())
