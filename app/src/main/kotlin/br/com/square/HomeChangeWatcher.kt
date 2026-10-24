package br.com.square

import com.eteks.sweethome3d.model.CollectionEvent
import com.eteks.sweethome3d.model.CollectionListener
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.HomeObject
import java.beans.PropertyChangeListener

/**
 * Avisa [onChange] quando o modelo muda, com os listeners originais do SH3D:
 * um CollectionListener por coleção do Home e um PropertyChangeListener em
 * cada item (mesmo padrão de swing/PlanComponent.addModelListeners). Ao
 * contrário do Swing, [close] remove tudo. [onChange] pode ser chamado de
 * qualquer thread e várias vezes seguidas; quem consome deve agrupar.
 */
class HomeChangeWatcher(home: Home, private val onChange: () -> Unit) : AutoCloseable {
    private val itemListener = PropertyChangeListener { onChange() }
    private val detach = mutableListOf<() -> Unit>()

    init {
        watch(home.walls, home::addWallsListener, home::removeWallsListener)
        watch(home.rooms, home::addRoomsListener, home::removeRoomsListener)
        watch(home.furniture, home::addFurnitureListener, home::removeFurnitureListener)
        watch(home.polylines, home::addPolylinesListener, home::removePolylinesListener)
        watch(home.dimensionLines, home::addDimensionLinesListener, home::removeDimensionLinesListener)
        watch(home.labels, home::addLabelsListener, home::removeLabelsListener)
        watch(home.levels, home::addLevelsListener, home::removeLevelsListener)
    }

    private fun <T : HomeObject> watch(
        items: Collection<T>,
        addCollectionListener: (CollectionListener<T>) -> Unit,
        removeCollectionListener: (CollectionListener<T>) -> Unit
    ) {
        // O Home troca a lista interna a cada add/delete (copy-on-write): `items`
        // fica velho, então os itens vigiados são rastreados aqui.
        val watched = mutableSetOf<T>()
        val collectionListener = CollectionListener<T> { ev ->
            when (ev.type) {
                CollectionEvent.Type.ADD -> if (watched.add(ev.item)) ev.item.addPropertyChangeListener(itemListener)
                CollectionEvent.Type.DELETE -> if (watched.remove(ev.item)) ev.item.removePropertyChangeListener(itemListener)
                else -> {}
            }
            onChange()
        }
        items.forEach { if (watched.add(it)) it.addPropertyChangeListener(itemListener) }
        addCollectionListener(collectionListener)
        detach += {
            removeCollectionListener(collectionListener)
            watched.forEach { it.removePropertyChangeListener(itemListener) }
            watched.clear()
        }
    }

    override fun close() {
        detach.forEach { it() }
        detach.clear()
    }
}
