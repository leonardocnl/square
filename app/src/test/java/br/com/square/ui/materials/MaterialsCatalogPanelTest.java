package br.com.square.ui.materials;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testes unitarios para o MaterialsCatalogPanel.
 */
public class MaterialsCatalogPanelTest {

    private Home home;
    private UserPreferences preferences;

    @Before
    public void setUp() {
        this.home = new Home();
        this.preferences = new DefaultUserPreferences();
    }

    @Test
    public void testMaterialsCatalogInitializationAndFiltering() {
        MaterialsCatalogPanel panel = new MaterialsCatalogPanel(home, preferences, null);
        assertNotNull("Painel deve ser inicializado", panel);

        // Deve carregar texturas do catalogo padrao
        int initialCount = panel.getListModel().getSize();
        org.junit.Assert.assertTrue("Deve conter texturas padrao", initialCount > 0);
    }

    @Test
    public void testApplyTextureToFurniture() {
        MaterialsCatalogPanel panel = new MaterialsCatalogPanel(home, preferences, null);

        CatalogPieceOfFurniture catPiece = new CatalogPieceOfFurniture("Mesa", null, null, 120f, 80f, 75f, true, false);
        HomePieceOfFurniture piece = new HomePieceOfFurniture(catPiece);
        home.addPieceOfFurniture(piece);
        home.setSelectedItems(Collections.singletonList(piece));

        // Seleciona a primeira textura da lista
        panel.getTextureList().setSelectedIndex(0);
        CatalogTexture selectedTexture = panel.getTextureList().getSelectedValue();
        assertNotNull("Textura deve existir", selectedTexture);

        panel.applySelectedTexture();

        assertNotNull("Movel deve ter recebido textura", piece.getTexture());
        assertEquals("Nome da textura deve coincidir", selectedTexture.getName(), piece.getTexture().getName());
    }

    @Test
    public void testApplyTextureToWall() {
        MaterialsCatalogPanel panel = new MaterialsCatalogPanel(home, preferences, null);

        Wall wall = new Wall(0f, 0f, 300f, 0f, 15f, 250f);
        home.addWall(wall);
        home.setSelectedItems(Collections.singletonList(wall));

        panel.getTextureList().setSelectedIndex(0);
        CatalogTexture selectedTexture = panel.getTextureList().getSelectedValue();

        panel.applySelectedTexture();

        assertNotNull("Parede deve ter recebido textura no lado esquerdo", wall.getLeftSideTexture());
        assertNotNull("Parede deve ter recebido textura no lado direito", wall.getRightSideTexture());
        assertEquals(selectedTexture.getName(), wall.getLeftSideTexture().getName());
    }

    @Test
    public void testApplyTextureToRoom() {
        MaterialsCatalogPanel panel = new MaterialsCatalogPanel(home, preferences, null);

        float[][] points = new float[][] { {0f, 0f}, {400f, 0f}, {400f, 400f}, {0f, 400f} };
        Room room = new Room(points);
        home.addRoom(room);
        home.setSelectedItems(Collections.singletonList(room));

        panel.getTextureList().setSelectedIndex(0);
        CatalogTexture selectedTexture = panel.getTextureList().getSelectedValue();

        panel.applySelectedTexture();

        assertNotNull("Comodo deve ter recebido textura de piso", room.getFloorTexture());
        assertEquals(selectedTexture.getName(), room.getFloorTexture().getName());
    }
}
