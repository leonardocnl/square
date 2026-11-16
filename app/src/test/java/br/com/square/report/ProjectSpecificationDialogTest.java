package br.com.square.report;

import br.com.square.ui.report.ProjectSpecificationDialog;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertNotNull;

/**
 * Testes unitarios para o dialogo executivo ProjectSpecificationDialog.
 */
public class ProjectSpecificationDialogTest {

    @Test
    public void testProjectSpecificationDialogLifecycle() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        Home home = new Home();
        home.addWall(new Wall(0f, 0f, 300f, 0f, 15f, 250f));
        Room room = new Room(new float[][]{{0f, 0f}, {300f, 0f}, {300f, 200f}, {0f, 200f}});
        room.setName("Cozinha");
        home.addRoom(room);

        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, home);
        assertNotNull("Dialogo deve ser instanciado", dialog);
        dialog.dispose();
    }
}
