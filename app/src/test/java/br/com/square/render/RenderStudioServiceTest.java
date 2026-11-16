package br.com.square.render;

import br.com.square.render.RenderStudioService.RenderQuality;
import br.com.square.render.RenderStudioService.ResolutionPreset;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o servico de renderizacao e iluminacao (RenderStudioService).
 */
public class RenderStudioServiceTest {

    private RenderStudioService service;
    private File tempImageFile;

    @Before
    public void setUp() throws IOException {
        service = new RenderStudioService();
        tempImageFile = File.createTempFile("render_test_", ".png");
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.shutdown();
        }
        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
        }
    }

    @Test
    public void testResolutionPresets() {
        assertEquals(1280, ResolutionPreset.HD_720P.getWidth());
        assertEquals(720, ResolutionPreset.HD_720P.getHeight());

        assertEquals(1920, ResolutionPreset.FULL_HD_1080P.getWidth());
        assertEquals(1080, ResolutionPreset.FULL_HD_1080P.getHeight());

        assertEquals(3840, ResolutionPreset.UHD_4K.getWidth());
        assertEquals(2160, ResolutionPreset.UHD_4K.getHeight());

        assertEquals(1080, ResolutionPreset.SQUARE_1080.getWidth());
        assertEquals(1080, ResolutionPreset.SQUARE_1080.getHeight());
    }

    @Test
    public void testRenderQualityLevels() {
        assertEquals(0, RenderQuality.DRAFT.getQualityLevel());
        assertEquals(1, RenderQuality.NORMAL.getQualityLevel());
        assertEquals(2, RenderQuality.HIGH.getQualityLevel());
        assertEquals(3, RenderQuality.ULTRA.getQualityLevel());
    }

    @Test
    public void testSunHourCalculations() {
        Home home = new Home();
        Camera camera = home.getCamera();
        assertNotNull(camera);

        // Define horario 15:30 (15.5)
        RenderStudioService.setSunHour(home, 15.5);
        double hour = RenderStudioService.getSunHour(home);
        assertEquals(15.5, hour, 0.05);

        // Define horario 08:00
        RenderStudioService.setSunHour(home, 8.0);
        hour = RenderStudioService.getSunHour(home);
        assertEquals(8.0, hour, 0.05);
    }

    @Test
    public void testCeilingLightsConfiguration() {
        Home home = new Home();

        RenderStudioService.setCeilingLightsEnabled(home, true);
        assertTrue("Luzes do teto devem estar ativas", RenderStudioService.isCeilingLightsEnabled(home));

        RenderStudioService.setCeilingLightsEnabled(home, false);
        assertFalse("Luzes do teto devem estar inativas", RenderStudioService.isCeilingLightsEnabled(home));
    }

    @Test
    public void testSaveImageToFile() throws IOException {
        BufferedImage testImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImg.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        RenderStudioService.saveImageToFile(testImg, tempImageFile, "png");
        assertTrue("Arquivo deve existir apos gravacao", tempImageFile.exists());
        assertTrue("Tamanho do arquivo deve ser positivo", tempImageFile.length() > 0);
    }

    @Test
    public void testCancelRenderLifecycle() {
        assertEquals(RenderStudioService.RenderStatus.IDLE, service.getCurrentStatus());
        service.cancelRender();
        assertEquals(RenderStudioService.RenderStatus.CANCELLED, service.getCurrentStatus());
    }
}
