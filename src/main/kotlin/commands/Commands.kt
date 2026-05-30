package dev.cypdashuhn.worldtasker.commands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.entity.Player

fun todo() = CommandAPICommand("todo")
    .executesPlayer(PlayerCommandExecutor { sender, args ->
        sender.sendMessage("TODO!")
    })
    .register()

