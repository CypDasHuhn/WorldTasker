package dev.cypdashuhn.worldtasker.db

import dev.rooster.core.RoosterModuleBuilder
import dev.rooster.db.db

fun RoosterModuleBuilder.initDb() {
    db(listOf(
        NamespaceManager.Namespaces,
        TagManager.Tags,
        TodoManager.Todos,
        TagManager.TodoTags,
        TagManager.TagInheritance,
        HistoryManager.History,
    ))
}
