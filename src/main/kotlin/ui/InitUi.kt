package dev.cypdashuhn.worldtasker.ui

import dev.rooster.core.RoosterModuleBuilder
import dev.rooster.ui.sql.SqlInterfaceContextProvider.Companion.addSqlInterfaceContextProvider
import dev.rooster.ui.ui

fun RoosterModuleBuilder.initUi() {
    services.addSqlInterfaceContextProvider()
    ui(listOf(TodoListInterface, TodoDetailInterface, TodoHistoryInterface, NamespaceSelectInterface, TagSelectInterface))
}