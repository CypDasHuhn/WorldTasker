package dev.cypdashuhn.worldtasker.commands.wrapper

import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.TextArgument

fun nameArgument(
    key: String,
    alreadyExists: (String) -> Boolean,
): Argument<String> =
    CustomArgument(TextArgument(key)) { info ->
        // if (alreadyExists(info.input)) throw error("Name already used: ", true)
        if (info.input == "[Name]") throw error("Name cannot be '[Name]', this is a placeholder.", true)
        info.input
    }.simpleSuggestions("[Name]")
