package br.com.square.render;

import br.com.square.ui.render.RenderStudioDialog;
import com.eteks.sweethome3d.io.FileUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.PhotoController;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import org.junit.Test;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertNotNull;

/**
 * Testes unitarios para o dialogo do RenderStudioDialog.
 */
public class RenderStudioDialogTest {

    @Test
    public void testRenderStudioDialogInstantiationAndDisplay() {
        Home home = new Home();
        UserPreferences preferences = new FileUserPreferences();
        ViewFactory viewFactory = new SwingViewFactory();
        View dummyView3D = new View() {};
        PhotoController photoController = new PhotoController(home, preferences,
                dummyView3D, viewFactory, null);

        RenderStudioDialog dialog = new RenderStudioDialog(home, preferences, photoController);
        assertNotNull("Dialogo deve ser instanciado", dialog);

        class DummyParentView extends JPanel implements View {}
        DummyParentView parent = new DummyParentView();
        dialog.displayView(parent);

        if (!GraphicsEnvironment.isHeadless()) {
            assertNotNull("JDialog interno deve ser criado em ambiente com display", dialog.getDialog());
            dialog.getDialog().dispose();
        }
    }
}
