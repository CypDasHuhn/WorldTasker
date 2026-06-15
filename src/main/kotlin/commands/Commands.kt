package dev.cypdashuhn.worldtasker.commands

import dev.cypdashuhn.worldtasker.commands.nodes.buildAddNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildEditNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildGetNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildHelpNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildInfoNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildJumpNode
// import dev.cypdashuhn.worldtasker.commands.nodes.buildProfilesNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildRemoveNode
import dev.cypdashuhn.worldtasker.commands.nodes.buildUiNode
import dev.cypdashuhn.worldtasker.commands.nodes.tags.buildTodoTagsNode
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor

fun todo() {
    CommandTree("todo")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            TodoListInterface.openInventory(sender)
        })
        .then(buildGetNode())
        .then(buildInfoNode())
        .then(buildAddNode())
        .then(buildEditNode())
        .then(buildRemoveNode())
        .then(buildJumpNode())
        .then(buildTodoTagsNode())
        // .then(buildProfilesNode())
        .then(buildHelpNode())
        .then(buildUiNode())
        .register()
}
