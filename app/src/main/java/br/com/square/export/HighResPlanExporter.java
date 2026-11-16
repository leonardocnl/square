package br.com.square.export;

import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Exportador de alta resolucao da planta 2D em formato raster (PNG / JPEG).
 * Gera pranchas graficas em definicao arquitetural para plotagem ou compartilhamento.
 */
public class HighResPlanExporter {

    private static final DecimalFormat DF;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DF = new DecimalFormat("0.#", symbols);
    }

    private HighResPlanExporter() {
    }

    public static BufferedImage renderToImage(Home home, int targetWidth, int targetHeight) {
        if (home == null) {
            throw new IllegalArgumentException("Home nao pode ser nulo");
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Dimensoes devem ser positivas");
        }

        // Calcula limites
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean hasElements = false;

        for (Wall wall : home.getWalls()) {
            minX = Math.min(minX, Math.min(wall.getXStart(), wall.getXEnd()));
            minY = Math.min(minY, Math.min(wall.getYStart(), wall.getYEnd()));
            maxX = Math.max(maxX, Math.max(wall.getXStart(), wall.getXEnd()));
            maxY = Math.max(maxY, Math.max(wall.getYStart(), wall.getYEnd()));
            hasElements = true;
        }

        for (Room room : home.getRooms()) {
            for (float[] p : room.getPoints()) {
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
                hasElements = true;
            }
        }

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (piece.isVisible()) {
                for (float[] p : piece.getPoints()) {
                    minX = Math.min(minX, p[0]);
                    minY = Math.min(minY, p[1]);
                    maxX = Math.max(maxX, p[0]);
                    maxY = Math.max(maxY, p[1]);
                    hasElements = true;
                }
            }
        }

        if (!hasElements) {
            minX = 0;
            minY = 0;
            maxX = 1000;
            maxY = 800;
        }

        float margin = 50.0f;
        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;

        float sceneWidth = maxX - minX;
        float sceneHeight = maxY - minY;

        float scaleX = targetWidth / sceneWidth;
        float scaleY = targetHeight / sceneHeight;
        float scale = Math.min(scaleX, scaleY);

        float offsetX = (targetWidth - (sceneWidth * scale)) / 2.0f;
        float offsetY = (targetHeight - (sceneHeight * scale)) / 2.0f;

        BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Fundo
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);

            AffineTransform tx = new AffineTransform();
            tx.translate(offsetX, offsetY);
            tx.scale(scale, scale);
            tx.translate(-minX, -minY);
            g2d.transform(tx);

            // 1. Comodos
            g2d.setColor(new Color(0xEE, 0xF2, 0xF6));
            for (Room room : home.getRooms()) {
                float[][] points = room.getPoints();
                if (points != null && points.length >= 3) {
                    GeneralPath path = new GeneralPath();
                    path.moveTo(points[0][0], points[0][1]);
                    for (int i = 1; i < points.length; i++) {
                        path.lineTo(points[i][0], points[i][1]);
                    }
                    path.closePath();
                    g2d.fill(path);
                }
            }

            // 2. Paredes
            g2d.setColor(new Color(0x1E, 0x29, 0x3B));
            for (Wall wall : home.getWalls()) {
                float[][] points = wall.getPoints();
                if (points != null && points.length >= 3) {
                    GeneralPath path = new GeneralPath();
                    path.moveTo(points[0][0], points[0][1]);
                    for (int i = 1; i < points.length; i++) {
                        path.lineTo(points[i][0], points[i][1]);
                    }
                    path.closePath();
                    g2d.fill(path);
                }
            }

            // 3. Moveis
            g2d.setColor(new Color(0x64, 0x74, 0x8B));
            for (HomePieceOfFurniture piece : home.getFurniture()) {
                if (piece.isVisible()) {
                    float[][] points = piece.getPoints();
                    if (points != null && points.length >= 3) {
                        GeneralPath path = new GeneralPath();
                        path.moveTo(points[0][0], points[0][1]);
                        for (int i = 1; i < points.length; i++) {
                            path.lineTo(points[i][0], points[i][1]);
                        }
                        path.closePath();
                        g2d.fill(path);
                    }
                }
            }

            // 4. Cotas
            g2d.setColor(new Color(0x02, 0x84, 0xC7));
            g2d.setStroke(new BasicStroke(1.5f / scale));
            for (DimensionLine dim : home.getDimensionLines()) {
                g2d.draw(new Line2D.Float(dim.getXStart(), dim.getYStart(), dim.getXEnd(), dim.getYEnd()));
            }

        } finally {
            g2d.dispose();
        }

        return image;
    }

    public static void exportToFile(Home home, int targetWidth, int targetHeight, File targetFile, String format) throws IOException {
        BufferedImage image = renderToImage(home, targetWidth, targetHeight);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        ImageIO.write(image, format != null ? format : "PNG", targetFile);
    }
}
