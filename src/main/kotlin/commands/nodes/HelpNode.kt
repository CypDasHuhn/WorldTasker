package dev.cypdashuhn.worldtasker.commands.nodes

import dev.cypdashuhn.worldtasker.commands.la
import dev.cypdashuhn.worldtasker.commands.msg
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

private const val TOPIC = "topic"

private fun br(lines: List<String>) = lines.joinToString("<br>")

private val topics = mapOf(
    "namespaces" to br(listOf(
        "<gold>=== Namespaces ===",
        "<gray>Namespaces are containers that group tags together.",
        "<gray>Each namespace has a name, display material, and a <white>single/multi tag mode</white>.",
        "<gray>  <white>Single mode</white> — a todo can have at most one tag from this namespace.",
        "<gray>  <white>Multi mode</white> — a todo can have any number of tags from this namespace (default).",
        "<gray>",
        "<gray><gold>Commands</gold>",
        "<gray>  <white>/todo tags namespaces list</white> — List all namespaces",
        "<gray>  <white>/todo tags namespaces add <name> [--single]</white> — Create a namespace",
        "<gray>  <white>/todo tags namespaces remove <name></white> — Delete a namespace",
        "<gray>  <white>/todo tags namespaces rename <old> <new></white> — Rename a namespace",
        "<gray>  <white>/todo tags namespaces info <name></white> — Show namespace details",
        "<gray>",
        "<gray>Open the namespace manager from the <white>[Namespaces]</white> button in the todo list.",
    )),
    "tags" to br(listOf(
        "<gold>=== Tags ===",
        "<gray>Tags are labels you attach to todos, written as <white>namespace:tagName</white>.",
        "<gray>They are used for filtering and visual identification.",
        "<gray>Each tag has a display material that you can customize.",
        "<gray>",
        "<gray><gold>Commands</gold>",
        "<gray>  <white>/todo tags list</white> — List all tags",
        "<gray>  <white>/todo tags add <namespace> <tagName></white> — Create a tag",
        "<gray>  <white>/todo tags remove <namespace:tagName></white> — Delete a tag",
        "<gray>  <white>/todo tags rename <namespace:tagName> <newName></white> — Rename a tag",
        "<gray>",
        "<gray>Assign tags to a todo from its detail view <white>[Tags]</white> button,",
        "<gray>or with <white>/todo edit <name> tags set/add/remove <tags...></white>.",
    )),
    "tag-inheritance" to br(listOf(
        "<gold>=== Tag Inheritance ===",
        "<gray>Tags can <white>inherit</white> from other tags (parent → child).",
        "<gray>When a todo has a child tag, it <white>also matches</white> all of its parent tags in filters.",
        "<gray>",
        "<gray>Example: tag <white>build:facade</white> inherits from <white>build:exterior</white>.",
        "<gray>  Filtering for <white>build:exterior</white> will also show todos with <white>facade</white>.",
        "<gray>",
        "<gray><gold>Commands</gold>",
        "<gray>  <white>/todo tags inherit <child> add <parents...></white> — Add parents",
        "<gray>  <white>/todo tags inherit <child> set <parents...></white> — Replace parents",
        "<gray>  <white>/todo tags inherit <child> remove <parents...></white> — Remove parents",
        "<gray>",
        "<gray>Manage inheritance from the <white>[Inheritance]</white> button in a tag's detail view.",
    )),
    "todo-namespacing" to br(listOf(
        "<gold>=== Todo Namespacing (TodoScope) ===",
        "<gray>When a <white>scope namespace</white> is configured, todo names become qualified",
        "<gray>by their scope tag. This means two todos named 'entrance' can coexist:",
        "<gray>  <white>spawn:entrance</white> and <white>market:entrance</white>.",
        "<gray>",
        "<gray><gold>Resolution rules</gold>",
        "<gray>  <white>/todo info entrance</white> — resolves to the only match, or shows options",
        "<gray>  <white>/todo info spawn:entrance</white> — explicitly scoped",
        "<gray>  <white>/todo info no-namespace:entrance</white> — the todo with no scope tag",
        "<gray>",
        "<gray>Applies to: <white>info</white>, <white>edit</white>, <white>jump</white>, <white>remove</white>.",
    )),
    "todo-history" to br(listOf(
        "<gold>=== Todo History ===",
        "<gray>Every todo change is tracked as a <white>history entry</white>.",
        "<gray>Todo state (active / completed / deleted) is derived from the latest entry.",
        "<gray>",
        "<gray><gold>History types</gold>",
        "<gray>  <white>CREATE</white> — Todo was created (author + location recorded)",
        "<gray>  <white>WORK</white> — Work was logged with an optional comment",
        "<gray>  <white>COMPLETE</white> — Todo was marked completed",
        "<gray>  <white>REACTIVATE</white> — Todo was reactivated from completed",
        "<gray>  <white>DELETE</white> — Todo was soft-deleted (not removed from DB)",
        "<gray>",
        "<gray>View history from the <white>[History]</white> button in a todo's detail view,",
        "<gray>or log work with <white>/todo edit <name> work <comment></white>.",
    )),
    "filters" to br(listOf(
        "<gold>=== Filters ===",
        "<gray>Filters narrow down the todo list. Open from the <white>[Filters]</white> button in the todo list.",
        "<gray>",
        "<gray><gold>Filter types</gold>",
        "<gray>  <white>Tag Filter</white> — 3-state per tag: Neutral → Include → Exclude",
        "<gray>  <white>Status</white> — Cycle: Default (active) → All → Completed",
        "<gray>  <white>Author</white> — 3-state per author: Neutral → Include → Exclude",
        "<gray>  <white>Distance</white> — Shift-click to toggle, left/right click to adjust radius",
        "<gray>",
        "<gray>Filters persist while you browse. Use <white>[Clear Filter]</white> to reset.",
    )),
    "search" to br(listOf(
        "<gold>=== Search ===",
        "<gray><white>/todo search</white> finds todos matching optional flags.",
        "<gray>All flags can be combined in any order:",
        "<gray>  <white>--name <substring></white> — Match by name (SQL LIKE)",
        "<gray>  <white>--author <name></white> — Match by exact author",
        "<gray>  <white>--near <chunk_radius></white> — Match by distance from you",
        "<gray>  <white>--time <type> <op> <date></white> — type: created/work/completed",
        "<gray>    op: before/after/on (or < > =), date: yyyy-MM-dd",
        "<gray>  <white>--tags <expression></white> — Use tag DSL: <white>tagA+tagB</white> (AND),",
        "<gray>    <white>tagA,tagB</white> (OR), <white>-tagA</white> (NOT), <white>()</white> for grouping",
        "<gray>  <white>--completed</white> — Include completed todos in results",
        "<gray>  <white>--random</white> — Show only one random matching todo",
        "<gray>",
        "<gray>Example: <white>/todo search --near 5 --tags 'spawn+lobby' --random</white>",
        "<gray>",
        "<gray>Tip: search respects tag inheritance — filtering for a parent finds children.",
    )),
)

private val overview = br(listOf(
    "<gold>=== Todo Help ===",
    "<gray>Manage tasks with tags, namespaces, filters, and inheritance.",
    "<gray>",
    "<gray><gold>Commands</gold>",
    "<gray>  <white>/todo</white> — Open the main todo list GUI",
    "<gray>  <white>/todo add <name> <desc> [tags...]</white> — Create a new todo",
    "<gray>  <white>/todo edit <name> ...</white> — Edit a todo (complete, rename, tags, etc.)",
    "<gray>  <white>/todo remove <name></white> — Delete a todo",
    "<gray>  <white>/todo info <name></white> — Show a todo's details in chat",
    "<gray>  <white>/todo jump <name></white> — Teleport to a todo's location",
    "<gray>  <white>/todo search [flags...]</white> — Search for todos",
    "<gray>  <white>/todo tags ...</white> — Manage tags and namespaces",
    "<gray>  <white>/todo help <topic></white> — Get help on a specific topic",
    "<gray>",
    "<gray><gold>Topics</gold>",
    "<gray>  <white>${topics.keys.joinToString(", ")}</white>",
))

internal fun buildHelpNode(): LiteralArgument =
    la("help").apply {
        executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.msg(overview)
        })
        then(
            StringArgument(TOPIC).replaceSuggestions(ArgumentSuggestions.strings { _ ->
                topics.keys.toTypedArray()
            }).executesPlayer(PlayerCommandExecutor { sender, args ->
                val topic = (args.argsMap[TOPIC] as String).lowercase()
                val helpText = topics[topic]
                    ?: "<red>Unknown topic '<white>$topic</white>'. Available: <white>${topics.keys.joinToString(", ")}</white>"
                sender.msg(helpText)
            })
        )
    }
