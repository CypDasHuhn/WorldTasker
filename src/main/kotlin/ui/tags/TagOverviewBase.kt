package dev.cypdashuhn.worldtasker.ui.tags

import dev.cypdashuhn.worldtasker.db.TagManager
import dev.rooster.ui.interfaces.ContextHandler
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import dev.rooster.ui.interfaces.constructors.indexed_content.ScrollInterfaceOptions
import org.bukkit.Material

data class TagData(
    val id: Int,
    val name: String,
    val material: Material,
)

abstract class TagOverviewBase<C : ScrollContext>(
    name: String,
    handler: ContextHandler<C>,
    options: ScrollInterfaceOptions<C>,
) : ScrollInterface<C, TagData>(name, handler, options) {
    abstract fun namespaceId(context: C): Int

    override fun contentProvider(id: Int, context: C): TagData? =
        TagManager.byNamespace(namespaceId(context)).getOrNull(id)?.let {
            TagData(
                it[TagManager.Tags.id].value,
                it[TagManager.Tags.name],
                Material.getMaterial(it[TagManager.Tags.material]) ?: Material.PAPER,
            )
        }
}
