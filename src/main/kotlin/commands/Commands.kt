package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.commands.nodes.buildAddNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildEditNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildGetNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildInfoNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildJumpNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildRemoveNode
import dev.cypdashuhn.worldtasker.commands.nodes.tags.buildTodoTagsNode
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor

fun todo() {
    CommandTree("todo")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.msg("<red>Usage: /todo <get|info|add|edit|remove|jump|tags>")
        })
        .then(buildGetNode())
        .then(buildInfoNode())
        .then(buildAddNode())
        .then(buildEditNode())
        .then(buildRemoveNode())
        .then(buildJumpNode())
        .then(buildTodoTagsNode())
        .register()
}
