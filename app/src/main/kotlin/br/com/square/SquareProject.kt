package br.com.square

import com.eteks.sweethome3d.io.DefaultUserPreferences
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.viewcontroller.HomeController3D
import com.eteks.sweethome3d.viewcontroller.PlanController
import javax.swing.undo.UndoableEditSupport

/** Um Home aberto: os controllers originais do SH3D e as views do Square ligadas a eles. */
class SquareProject(val home: Home) : AutoCloseable {
    private val preferences = DefaultUserPreferences()
    private val factory = SquareViewFactory()
    private val undoSupport = UndoableEditSupport()
    val planController = PlanController(home, preferences, factory, null, undoSupport)
    val home3DController = HomeController3D(home, planController, preferences, factory, null, undoSupport)

    val planView: SquarePlanView get() = planController.view as SquarePlanView
    val view3D: Square3DView get() = home3DController.view as Square3DView

    override fun close() {
        planView.dispose()
        view3D.dispose()
    }
}
