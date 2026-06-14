package dev.cypdashuhn.worldtasker.ui

import dev.cypdashuhn.worldtasker.ui.filters.AuthorInterface
import dev.cypdashuhn.worldtasker.ui.filters.FiltersInterface
import dev.cypdashuhn.worldtasker.ui.filters.ProfileListInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.DeleteNamespaceConfirmation
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceAssignInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceEditInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceInheritanceInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.NamespaceSelectInterface
import dev.cypdashuhn.worldtasker.ui.namespaces.RenameNamespaceConfirmation
import dev.cypdashuhn.worldtasker.ui.tags.DeleteTagConfirmation
import dev.cypdashuhn.worldtasker.ui.tags.RemoveTaggingsDeleteTagConfirmation
import dev.cypdashuhn.worldtasker.ui.tags.RemoveTodosDeleteTagConfirmation
import dev.cypdashuhn.worldtasker.ui.tags.RenameTagConfirmation
import dev.cypdashuhn.worldtasker.ui.tags.TagAssignInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagDeleteConflictInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagDetailInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagEditInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagInheritanceInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagInheritanceSelectInterface
import dev.cypdashuhn.worldtasker.ui.tags.TagSelectInterface
import dev.cypdashuhn.worldtasker.ui.tags.ToggleTagModeConfirmation
import dev.cypdashuhn.worldtasker.ui.todo.DeleteTodoConfirmation
import dev.cypdashuhn.worldtasker.ui.todo.RenameTodoConfirmation
import dev.cypdashuhn.worldtasker.ui.todo.TodoDetailInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoHistoryInterface
import dev.cypdashuhn.worldtasker.ui.todo.TodoListInterface
import dev.rooster.core.RoosterModuleBuilder
import dev.rooster.ui.sql.SqlInterfaceContextProvider.Companion.addSqlInterfaceContextProvider
import dev.rooster.ui.ui

fun RoosterModuleBuilder.initUi() {
    services.addSqlInterfaceContextProvider()
    ui(
        listOf(
            // Todo flow
            TodoListInterface,
            TodoDetailInterface,
            TodoHistoryInterface,
            // Filters
            FiltersInterface,
            ProfileListInterface,
            AuthorInterface,
            // Namespace overview modes
            NamespaceSelectInterface,
            NamespaceEditInterface,
            NamespaceAssignInterface,
            // Tag overview modes
            TagSelectInterface,
            TagEditInterface,
            TagAssignInterface,
            // Tag detail & inheritance
            TagDeleteConflictInterface,
            TagDetailInterface,
            TagInheritanceInterface,
            NamespaceInheritanceInterface,
            TagInheritanceSelectInterface,
            // Change material
            ChangeTagMaterialInterface,
            ChangeNamespaceMaterialInterface,
            // Confirmations
            DeleteTodoConfirmation,
            RenameTodoConfirmation,
            DeleteNamespaceConfirmation,
            RenameNamespaceConfirmation,
            DeleteTagConfirmation,
            RenameTagConfirmation,
            RemoveTaggingsDeleteTagConfirmation,
            RemoveTodosDeleteTagConfirmation,
            ToggleTagModeConfirmation,
        ),
    )
}
