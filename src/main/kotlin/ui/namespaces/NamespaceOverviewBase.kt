package dev.cypdashuhn.worldtasker.ui.namespaces

import dev.cypdashuhn.worldtasker.db.NamespaceManager
import dev.rooster.ui.interfaces.ContextHandler
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import org.bukkit.Material

data class NamespaceData(
    val id: Int,
    val name: String,
    val material: Material,
    val allowsMultiple: Boolean,
)

abstract class NamespaceOverviewBase<C : ScrollContext>(
    handler: ContextHandler<C>,
    options: ScrollInterfaceOptions<C>,
) : ScrollInterface<C, NamespaceData>(handler, options) {
    override fun contentProvider(id: Int, context: C): NamespaceData? =
        NamespaceManager.all().getOrNull(id)?.let {
            NamespaceData(
                it[NamespaceManager.Namespaces.id].value,
                it[NamespaceManager.Namespaces.name],
                Material.getMaterial(it[NamespaceManager.Namespaces.material]) ?: Material.BOOKSHELF,
                it[NamespaceManager.Namespaces.allowsMultiple],
            )
        }
}
