package dev.cypdashuhn.worldtasker.commands

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor

fun todo() {
    CommandTree("todo")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.msg("<red>Usage: /todo <get|add|edit|remove|jump|tags>")
        })
        .then(buildGetNode())
        .then(buildAddNode())
        .then(buildEditNode())
        .then(buildRemoveNode())
        .then(buildJumpNode())
        .then(buildTodoTagsNode())
        .register()
}
