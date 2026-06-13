package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.actions.QueryProfileActions
import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.db.ProfileSaveResult
import dev.cypdashuhn.worldtasker.db.QueryProfileManager
import dev.cypdashuhn.worldtasker.db.TodoFilter
import dev.cypdashuhn.worldtasker.commands.msg
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val PROFILE_NAME = "queryProfileName"

internal fun buildProfilesNode(): LiteralArgument =
    la("profiles").apply {
        then(la("list").executesPlayer(PlayerCommandExecutor { sender, _ ->
            QueryProfileActions.list(sender)
        }))
        then(la("create").then(
            StringArgument(PROFILE_NAME).executesPlayer(PlayerCommandExecutor { sender, args ->
                val name = args.argsMap[PROFILE_NAME] as String
                val trimmed = name.trim()
                if (trimmed.isBlank()) {
                    sender.msg("<red>Profile name cannot be empty.")
                    return@PlayerCommandExecutor
                }
                when (QueryProfileManager.save(trimmed, TodoFilter())) {
                    ProfileSaveResult.Saved -> sender.msg("<green>Query profile '<white>$trimmed</white>' created.")
                    ProfileSaveResult.DuplicateName -> sender.msg("<red>A profile named '<white>$trimmed</white>' already exists.")
                }
            })
        ))
        then(la("delete").then(
            StringArgument(PROFILE_NAME).executesPlayer(PlayerCommandExecutor { sender, args ->
                QueryProfileActions.delete(sender, args.argsMap[PROFILE_NAME] as String)
            })
        ))
        then(la("apply").then(
            StringArgument(PROFILE_NAME).executesPlayer(PlayerCommandExecutor { sender, args ->
                QueryProfileActions.apply(sender, args.argsMap[PROFILE_NAME] as String)
            })
        ))
    }
