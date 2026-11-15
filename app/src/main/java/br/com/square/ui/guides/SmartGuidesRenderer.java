package br.com.square.ui.guides;

import br.com.square.ui.theme.ThemeManager;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.Wall;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renderizador de Guias Inteligentes (Smart Guides) e Réguas Dinâmicas na Planta 2D.
 * Detecta e exibe linhas de alinhamento magnético e medidas de distância
 * em tempo real quando elementos são manipulados na planta.
 */
public final class SmartGuidesRenderer {

    private static final float ALIGN_SNAP_THRESHOLD_CM = 4.0f;
    private static final float MAX_DISTANCE_MEASURE_CM = 250.0f;

    private static final Color LIGHT_GUIDE_COLOR = new Color(0xE1, 0x1D, 0x48, 220); // Rose-600
    private static final Color DARK_GUIDE_COLOR = new Color(0xFB, 0x71, 0x85, 230);  // Rose-400
    private static final Color LIGHT_MEASURE_BG = new Color(0x18, 0x18, 0x1B, 200);
    private static final Color DARK_MEASURE_BG = new Color(0x27, 0x27, 0x2A, 220);

    private static final DecimalFormat DISTANCE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DISTANCE_FORMAT = new DecimalFormat("#.#", symbols);
    }

    private SmartGuidesRenderer() {
    }

    /**
     * Pinta as linhas guias de alinhamento e cotas dinâmicas de distância sobre a planta 2D.
     */
    public static void paintGuides(Graphics2D g2D, Home home, List<? extends Selectable> activeItems, float planScale) {
        if (home == null || activeItems == null || activeItems.isEmpty() || planScale <= 0) {
            return;
        }

        Rectangle2D activeBounds = computeItemsBounds(activeItems);
        if (activeBounds == null) {
            return;
        }

        List<Rectangle2D> neighborBounds = collectNeighborBounds(home, activeItems);
        if (neighborBounds.isEmpty()) {
            return;
        }

        Graphics2D g = (Graphics2D) g2D.create();
        boolean dark = ThemeManager.isDark();
        Color guideColor = dark ? DARK_GUIDE_COLOR : LIGHT_GUIDE_COLOR;
        Stroke guideStroke = new BasicStroke(1.0f / planScale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{4.0f / planScale, 3.0f / planScale}, 0);

        g.setColor(guideColor);
        g.setStroke(guideStroke);

        float activeMinX = (float) activeBounds.getMinX();
        float activeCenterX = (float) activeBounds.getCenterX();
        float activeMaxX = (float) activeBounds.getMaxX();

        float activeMinY = (float) activeBounds.getMinY();
        float activeCenterY = (float) activeBounds.getCenterY();
        float activeMaxY = (float) activeBounds.getMaxY();

        // 1. Alinhamentos Verticais (Eixo X)
        for (Rectangle2D neighbor : neighborBounds) {
            float nMinX = (float) neighbor.getMinX();
            float nCenterX = (float) neighbor.getCenterX();
            float nMaxX = (float) neighbor.getMaxX();

            checkAndDrawVerticalGuide(g, activeMinX, nMinX, activeBounds, neighbor);
            checkAndDrawVerticalGuide(g, activeCenterX, nCenterX, activeBounds, neighbor);
            checkAndDrawVerticalGuide(g, activeMaxX, nMaxX, activeBounds, neighbor);
            checkAndDrawVerticalGuide(g, activeMinX, nMaxX, activeBounds, neighbor);
            checkAndDrawVerticalGuide(g, activeMaxX, nMinX, activeBounds, neighbor);
        }

        // 2. Alinhamentos Horizontais (Eixo Y)
        for (Rectangle2D neighbor : neighborBounds) {
            float nMinY = (float) neighbor.getMinY();
            float nCenterY = (float) neighbor.getCenterY();
            float nMaxY = (float) neighbor.getMaxY();

            checkAndDrawHorizontalGuide(g, activeMinY, nMinY, activeBounds, neighbor);
            checkAndDrawHorizontalGuide(g, activeCenterY, nCenterY, activeBounds, neighbor);
            checkAndDrawHorizontalGuide(g, activeMaxY, nMaxY, activeBounds, neighbor);
            checkAndDrawHorizontalGuide(g, activeMinY, nMaxY, activeBounds, neighbor);
            checkAndDrawHorizontalGuide(g, activeMaxY, nMinY, activeBounds, neighbor);
        }

        // 3. Medições Dinâmicas de Folga / Distância mais próxima
        paintNearestDistances(g, activeBounds, neighborBounds, planScale, dark);

        g.dispose();
    }

    private static void checkAndDrawVerticalGuide(Graphics2D g, float activeX, float targetX,
                                                  Rectangle2D b1, Rectangle2D b2) {
        if (Math.abs(activeX - targetX) <= ALIGN_SNAP_THRESHOLD_CM) {
            float minY = (float) Math.min(b1.getMinY(), b2.getMinY()) - 20f;
            float maxY = (float) Math.max(b1.getMaxY(), b2.getMaxY()) + 20f;
            g.draw(new Line2D.Float(targetX, minY, targetX, maxY));
        }
    }

    private static void checkAndDrawHorizontalGuide(Graphics2D g, float activeY, float targetY,
                                                    Rectangle2D b1, Rectangle2D b2) {
        if (Math.abs(activeY - targetY) <= ALIGN_SNAP_THRESHOLD_CM) {
            float minX = (float) Math.min(b1.getMinX(), b2.getMinX()) - 20f;
            float maxX = (float) Math.max(b1.getMaxX(), b2.getMaxX()) + 20f;
            g.draw(new Line2D.Float(minX, targetY, maxX, targetY));
        }
    }

    private static void paintNearestDistances(Graphics2D g, Rectangle2D active, List<Rectangle2D> neighbors,
                                              float planScale, boolean dark) {
        float minGapRight = Float.MAX_VALUE;
        float rightTargetY = (float) active.getCenterY();
        float minGapBottom = Float.MAX_VALUE;
        float bottomTargetX = (float) active.getCenterX();

        for (Rectangle2D n : neighbors) {
            // Vizinho à direita com sobreposição vertical
            if (n.getMinX() >= active.getMaxX()) {
                float gap = (float) (n.getMinX() - active.getMaxX());
                if (gap < minGapRight && gap < MAX_DISTANCE_MEASURE_CM) {
                    if (hasVerticalOverlap(active, n)) {
                        minGapRight = gap;
                    }
                }
            }

            // Vizinho abaixo com sobreposição horizontal
            if (n.getMinY() >= active.getMaxY()) {
                float gap = (float) (n.getMinY() - active.getMaxY());
                if (gap < minGapBottom && gap < MAX_DISTANCE_MEASURE_CM) {
                    if (hasHorizontalOverlap(active, n)) {
                        minGapBottom = gap;
                    }
                }
            }
        }

        // Desenha cota horizontal à direita
        if (minGapRight < MAX_DISTANCE_MEASURE_CM && minGapRight > 1.0f) {
            float startX = (float) active.getMaxX();
            float endX = startX + minGapRight;
            drawDistanceIndicator(g, startX, rightTargetY, endX, rightTargetY, minGapRight, planScale, dark, true);
        }

        // Desenha cota vertical abaixo
        if (minGapBottom < MAX_DISTANCE_MEASURE_CM && minGapBottom > 1.0f) {
            float startY = (float) active.getMaxY();
            float endY = startY + minGapBottom;
            drawDistanceIndicator(g, bottomTargetX, startY, bottomTargetX, endY, minGapBottom, planScale, dark, false);
        }
    }

    private static void drawDistanceIndicator(Graphics2D g, float x1, float y1, float x2, float y2,
                                              float distanceCm, float planScale, boolean dark, boolean horizontal) {
        Stroke solid = new BasicStroke(1.0f / planScale);
        g.setStroke(solid);
        g.setColor(dark ? DARK_GUIDE_COLOR : LIGHT_GUIDE_COLOR);

        g.draw(new Line2D.Float(x1, y1, x2, y2));

        // Rótulo numérico
        String text = DISTANCE_FORMAT.format(distanceCm) + " cm";
        Font origFont = g.getFont();
        float fontSize = Math.max(8f / planScale, 10f);
        Font font = origFont.deriveFont(fontSize);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        float midX = (x1 + x2) / 2f;
        float midY = (y1 + y2) / 2f;

        // Fundo do badge
        float pad = 2f / planScale;
        float badgeW = textWidth + pad * 4;
        float badgeH = textHeight + pad * 2;
        float badgeX = midX - badgeW / 2;
        float badgeY = midY - badgeH / 2;

        g.setColor(dark ? DARK_MEASURE_BG : LIGHT_MEASURE_BG);
        g.fill(new Rectangle2D.Float(badgeX, badgeY, badgeW, badgeH));

        g.setColor(Color.WHITE);
        g.drawString(text, midX - textWidth / 2f, midY + textHeight / 3f);
        g.setFont(origFont);
    }

    private static boolean hasVerticalOverlap(Rectangle2D r1, Rectangle2D r2) {
        return Math.max(r1.getMinY(), r2.getMinY()) < Math.min(r1.getMaxY(), r2.getMaxY());
    }

    private static boolean hasHorizontalOverlap(Rectangle2D r1, Rectangle2D r2) {
        return Math.max(r1.getMinX(), r2.getMinX()) < Math.min(r1.getMaxX(), r2.getMaxX());
    }

    private static Rectangle2D computeItemsBounds(List<? extends Selectable> items) {
        Rectangle2D bounds = null;
        for (Selectable item : items) {
            Rectangle2D itemBounds = getItemBounds(item);
            if (itemBounds != null) {
                if (bounds == null) {
                    bounds = (Rectangle2D) itemBounds.clone();
                } else {
                    bounds.add(itemBounds);
                }
            }
        }
        return bounds;
    }

    private static List<Rectangle2D> collectNeighborBounds(Home home, List<? extends Selectable> excluded) {
        List<Rectangle2D> list = new ArrayList<Rectangle2D>();

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (!excluded.contains(piece) && piece.isVisible()) {
                Rectangle2D b = getItemBounds(piece);
                if (b != null) {
                    list.add(b);
                }
            }
        }

        for (Wall wall : home.getWalls()) {
            if (!excluded.contains(wall)) {
                Rectangle2D b = getItemBounds(wall);
                if (b != null) {
                    list.add(b);
                }
            }
        }

        return list;
    }

    private static Rectangle2D getItemBounds(Selectable item) {
        if (item instanceof HomePieceOfFurniture) {
            HomePieceOfFurniture p = (HomePieceOfFurniture) item;
            float[][] points = p.getPoints();
            return computePointsBounds(points);
        } else if (item instanceof Wall) {
            Wall w = (Wall) item;
            float[][] points = w.getPoints();
            return computePointsBounds(points);
        }
        return null;
    }

    private static Rectangle2D computePointsBounds(float[][] points) {
        if (points == null || points.length == 0) {
            return null;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (float[] pt : points) {
            if (pt != null && pt.length >= 2) {
                if (pt[0] < minX) minX = pt[0];
                if (pt[0] > maxX) maxX = pt[0];
                if (pt[1] < minY) minY = pt[1];
                if (pt[1] > maxY) maxY = pt[1];
            }
        }

        if (minX > maxX || minY > maxY) {
            return null;
        }

        return new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
    }
}

