package br.com.square

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.eteks.sweethome3d.model.DimensionLine
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.HomePieceOfFurniture
import com.eteks.sweethome3d.model.Selectable
import com.eteks.sweethome3d.model.TextStyle
import com.eteks.sweethome3d.viewcontroller.ExportableView
import com.eteks.sweethome3d.viewcontroller.PlanController
import com.eteks.sweethome3d.viewcontroller.PlanView
import com.eteks.sweethome3d.viewcontroller.TransferableView
import com.eteks.sweethome3d.viewcontroller.View
import java.io.OutputStream
import java.util.Properties
import kotlin.math.min

/** Fator de zoom por clique da roda (diferente do SH3D original: lá a roda rola). */
const val ZOOM_STEP = 1.1f
private const val FIT_MARGIN_PX = 40f

/** Offset (pixel da origem do modelo) que mantém o ponto sob [anchor] parado ao mudar a escala. */
fun offsetAfterZoom(offset: Offset, oldScale: Float, newScale: Float, anchor: Offset): Offset =
    anchor - (anchor - offset) * (newScale / oldScale)

/** Escala e offset que enquadram [bounds] (em cm) numa área de [size] pixels. */
fun fitCamera(bounds: Rect, size: Size, margin: Float = FIT_MARGIN_PX): Pair<Float, Offset> {
    val scale = min(
        (size.width - 2 * margin) / bounds.width.coerceAtLeast(1f),
        (size.height - 2 * margin) / bounds.height.coerceAtLeast(1f)
    )
    return scale to (Offset(size.width, size.height) / 2f - bounds.center * scale)
}

/**
 * PlanView do Square. A escala mora aqui (como no SH3D); os limites e o
 * zoom passam pelo PlanController original. O offset (pan) é o "viewport".
 */
class SquarePlanView(private val home: Home, private val controller: PlanController) : PlanView {
    private var scaleState by mutableFloatStateOf(0.5f)
    /** Posição em pixels da origem do modelo (0, 0). */
    var offset by mutableStateOf(Offset.Zero)
    var viewSize by mutableStateOf(Size.Zero)
        private set

    /**
     * Sobe a cada mudança do modelo; o canvas lê no desenho, então o Compose
     * junta várias mudanças seguidas num único redesenho por frame.
     */
    var version by mutableIntStateOf(0)
        private set
    /** Quantas vezes o canvas desenhou (usado pelos testes). */
    var drawCount = 0
        internal set

    private val watcher = HomeChangeWatcher(home, onChange = { version++ })

    /** Remove os listeners do Home; chame ao descartar a view. */
    fun dispose() = watcher.close()

    private var zoomAnchor: Offset? = null
    private var fitted = false

    override fun getScale() = scaleState

    override fun setScale(scale: Float) {
        val anchor = zoomAnchor ?: Offset(viewSize.width, viewSize.height) / 2f
        offset = offsetAfterZoom(offset, scaleState, scale, anchor)
        scaleState = scale
    }

    /** Zoom pelo controller original, mantendo o ponto sob [anchor] parado. */
    fun zoomAt(factor: Float, anchor: Offset) {
        zoomAnchor = anchor
        try {
            controller.zoom(factor)
        } finally {
            zoomAnchor = null
        }
    }

    fun pan(deltaPx: Offset) {
        offset += deltaPx
    }

    /** Enquadra o projeto inteiro. Sem itens, não faz nada. */
    fun fit() {
        val bounds = planBounds() ?: return
        if (viewSize.width <= 0f || viewSize.height <= 0f) return
        controller.setScale(fitCamera(bounds, viewSize).first) // aplica os limites do controller
        offset = Offset(viewSize.width, viewSize.height) / 2f - bounds.center * scaleState
    }

    /** Chamado pelo canvas; enquadra uma vez, quando o tamanho passa a ser conhecido. */
    fun onSize(size: Size) {
        viewSize = size
        if (!fitted && size.width > 0f && size.height > 0f && planBounds() != null) {
            fitted = true
            fit()
        }
    }

    private fun planBounds(): Rect? {
        val items: List<Selectable> =
            home.walls + home.rooms + home.furniture + home.dimensionLines + home.labels + home.polylines
        val points = items.flatMap { it.points.asList() }
        if (points.isEmpty()) return null
        return Rect(
            points.minOf { it[0] }, points.minOf { it[1] },
            points.maxOf { it[0] }, points.maxOf { it[1] }
        )
    }

    override fun moveView(dx: Float, dy: Float) {
        offset -= Offset(dx, dy) * scaleState
    }

    override fun convertXPixelToModel(x: Int) = (x - offset.x) / scaleState
    override fun convertYPixelToModel(y: Int) = (y - offset.y) / scaleState
    override fun convertXModelToScreen(x: Float) = (x * scaleState + offset.x).toInt()
    override fun convertYModelToScreen(y: Float) = (y * scaleState + offset.y).toInt()
    override fun getPixelLength() = 1f / scaleState

    // Parcial: os itens abaixo ainda não são usados no Marco 0 (ver VIEWFACTORY_COVERAGE.md).
    // Feedbacks de edição são no-op; o que devolve dados falha alto com TODO().
    override fun setRectangleFeedback(x0: Float, y0: Float, x1: Float, y1: Float) {} // TODO feedback
    override fun makeSelectionVisible() {} // TODO rolar até a seleção
    override fun makePointVisible(x: Float, y: Float) {} // TODO rolar até o ponto
    override fun getPrintPreferredScale(preferredWidth: Float, preferredHeight: Float): Float = TODO("impressão")
    override fun getTextBounds(text: String?, style: TextStyle?, x: Float, y: Float, angle: Float): Array<FloatArray> =
        TODO("medição de texto")
    override fun setCursor(cursorType: PlanView.CursorType?) {} // TODO cursores
    override fun setToolTipFeedback(toolTipFeedback: String?, x: Float, y: Float) {} // TODO
    override fun setToolTipEditedProperties(
        toolTipEditedProperties: Array<PlanController.EditableProperty>?,
        toolTipPropertyValues: Array<Any>?, x: Float, y: Float
    ) {} // TODO
    override fun setToolTipEditedPropertyValue(
        toolTipEditedProperty: PlanController.EditableProperty?, toolTipPropertyValue: Any?
    ) {} // TODO
    override fun deleteToolTipFeedback() {} // TODO
    override fun setResizeIndicatorVisible(resizeIndicatorVisible: Boolean) {} // TODO
    override fun setAlignmentFeedback(
        alignedObjectClass: Class<out Selectable>?, alignedObject: Selectable?,
        x: Float, y: Float, showPoint: Boolean
    ) {} // TODO
    override fun setAngleFeedback(
        xCenter: Float, yCenter: Float, x1: Float, y1: Float, x2: Float, y2: Float
    ) {} // TODO
    override fun setDraggedItemsFeedback(draggedItems: MutableList<Selectable>?) {} // TODO
    override fun setDimensionLinesFeedback(dimensionLines: MutableList<DimensionLine>?) {} // TODO
    override fun deleteFeedback() {} // TODO
    override fun getHorizontalRuler(): View = TODO("régua")
    override fun getVerticalRuler(): View = TODO("régua")
    override fun canImportDraggedItems(items: MutableList<Selectable>?, x: Int, y: Int): Boolean = false
    override fun getPieceOfFurnitureSizeInPlan(piece: HomePieceOfFurniture?): FloatArray = TODO("móveis na planta")
    override fun isFurnitureSizeInPlanSupported() = false
    override fun createTransferData(dataType: TransferableView.DataType?): Any = TODO("copiar/colar")
    override fun isFormatTypeSupported(formatType: ExportableView.FormatType?) = false
    override fun exportData(out: OutputStream?, formatType: ExportableView.FormatType?, settings: Properties?) =
        TODO("exportação")
}
