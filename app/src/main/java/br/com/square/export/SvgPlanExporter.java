package br.com.square.export;

import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Exportador vetorial de alta fidelidade da planta 2D para o formato SVG (Scalable Vector Graphics).
 * Gera arquivos limpos com camadas estruturadas (comodos, paredes, moveis, cotas e anotacoes).
 */
public class SvgPlanExporter {

    private static final DecimalFormat DF;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DF = new DecimalFormat("0.##", symbols);
    }

    private SvgPlanExporter() {
    }

    /**
     * Exporta o projeto arquitetural em uma string contendo documento SVG completo.
     */
    public static String exportToSvg(Home home) {
        if (home == null) {
            throw new IllegalArgumentException("Home nao pode ser nulo");
        }

        // 1. Calculo dos limites geometricos globais (Bounding Box)
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        boolean hasGeometry = false;

        for (Wall wall : home.getWalls()) {
            minX = Math.min(minX, Math.min(wall.getXStart(), wall.getXEnd()));
            minY = Math.min(minY, Math.min(wall.getYStart(), wall.getYEnd()));
            maxX = Math.max(maxX, Math.max(wall.getXStart(), wall.getXEnd()));
            maxY = Math.max(maxY, Math.max(wall.getYStart(), wall.getYEnd()));
            hasGeometry = true;
        }

        for (Room room : home.getRooms()) {
            for (float[] p : room.getPoints()) {
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
                hasGeometry = true;
            }
        }

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (piece.isVisible()) {
                for (float[] p : piece.getPoints()) {
                    minX = Math.min(minX, p[0]);
                    minY = Math.min(minY, p[1]);
                    maxX = Math.max(maxX, p[0]);
                    maxY = Math.max(maxY, p[1]);
                    hasGeometry = true;
                }
            }
        }

        for (DimensionLine dim : home.getDimensionLines()) {
            minX = Math.min(minX, Math.min(dim.getXStart(), dim.getXEnd()));
            minY = Math.min(minY, Math.min(dim.getYStart(), dim.getYEnd()));
            maxX = Math.max(maxX, Math.max(dim.getXStart(), dim.getXEnd()));
            maxY = Math.max(maxY, Math.max(dim.getYStart(), dim.getYEnd()));
            hasGeometry = true;
        }

        for (Label label : home.getLabels()) {
            minX = Math.min(minX, label.getX());
            minY = Math.min(minY, label.getY());
            maxX = Math.max(maxX, label.getX());
            maxY = Math.max(maxY, label.getY());
            hasGeometry = true;
        }

        if (!hasGeometry) {
            minX = 0;
            minY = 0;
            maxX = 1000;
            maxY = 800;
        }

        float margin = 60.0f;
        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;

        float width = maxX - minX;
        float height = maxY - minY;

        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
        svg.append("viewBox=\"").append(DF.format(minX)).append(" ").append(DF.format(minY)).append(" ")
                .append(DF.format(width)).append(" ").append(DF.format(height)).append("\" ");
        svg.append("width=\"").append(DF.format(width)).append("px\" ");
        svg.append("height=\"").append(DF.format(height)).append("px\">\n");

        // Estilos CSS incorporados
        svg.append("  <style>\n");
        svg.append("    text { font-family: system-ui, -apple-system, sans-serif; }\n");
        svg.append("    .room-polygon { fill: #F1F5F9; fill-opacity: 0.6; stroke: #94A3B8; stroke-width: 1.5; }\n");
        svg.append("    .room-name { font-size: 14px; font-weight: 600; fill: #1E293B; text-anchor: middle; }\n");
        svg.append("    .room-area { font-size: 11px; font-weight: 400; fill: #64748B; text-anchor: middle; }\n");
        svg.append("    .wall-polygon { fill: #0F172A; stroke: #020617; stroke-width: 1.5; }\n");
        svg.append("    .furniture-polygon { fill: #475569; fill-opacity: 0.85; stroke: #1E293B; stroke-width: 1.2; }\n");
        svg.append("    .furniture-label { font-size: 10px; font-weight: 500; fill: #FFFFFF; text-anchor: middle; }\n");
        svg.append("    .dim-line { stroke: #0284C7; stroke-width: 1.5; stroke-dasharray: 4, 3; }\n");
        svg.append("    .dim-label { font-size: 11px; font-weight: 600; fill: #0284C7; text-anchor: middle; }\n");
        svg.append("    .annotation-label { font-size: 12px; font-weight: 500; fill: #0F172A; }\n");
        svg.append("  </style>\n\n");

        // Fundo sutil
        svg.append("  <rect x=\"").append(DF.format(minX)).append("\" y=\"").append(DF.format(minY))
                .append("\" width=\"").append(DF.format(width)).append("\" height=\"").append(DF.format(height))
                .append("\" fill=\"#FFFFFF\" />\n\n");

        // Camada 1: Comodos (Rooms)
        svg.append("  <!-- Camada: Comodos -->\n");
        svg.append("  <g id=\"rooms\">\n");
        for (Room room : home.getRooms()) {
            float[][] points = room.getPoints();
            if (points != null && points.length >= 3) {
                StringBuilder pointsStr = new StringBuilder();
                float sumX = 0;
                float sumY = 0;
                for (float[] p : points) {
                    if (pointsStr.length() > 0) pointsStr.append(" ");
                    pointsStr.append(DF.format(p[0])).append(",").append(DF.format(p[1]));
                    sumX += p[0];
                    sumY += p[1];
                }
                float centerX = sumX / points.length;
                float centerY = sumY / points.length;

                svg.append("    <polygon class=\"room-polygon\" points=\"").append(pointsStr).append("\" />\n");

                String name = room.getName();
                float area = room.getArea();
                if (name != null && !name.trim().isEmpty()) {
                    svg.append("    <text class=\"room-name\" x=\"").append(DF.format(centerX))
                            .append("\" y=\"").append(DF.format(centerY - 5)).append("\">")
                            .append(escapeXml(name)).append("</text>\n");
                }
                if (area > 0) {
                    double areaM2 = area / 10000.0;
                    svg.append("    <text class=\"room-area\" x=\"").append(DF.format(centerX))
                            .append("\" y=\"").append(DF.format(centerY + 12)).append("\">")
                            .append(DF.format(areaM2)).append(" m&#178;</text>\n");
                }
            }
        }
        svg.append("  </g>\n\n");

        // Camada 2: Paredes (Walls)
        svg.append("  <!-- Camada: Paredes -->\n");
        svg.append("  <g id=\"walls\">\n");
        for (Wall wall : home.getWalls()) {
            float[][] points = wall.getPoints();
            if (points != null && points.length >= 3) {
                StringBuilder pointsStr = new StringBuilder();
                for (float[] p : points) {
                    if (pointsStr.length() > 0) pointsStr.append(" ");
                    pointsStr.append(DF.format(p[0])).append(",").append(DF.format(p[1]));
                }
                svg.append("    <polygon class=\"wall-polygon\" points=\"").append(pointsStr).append("\" />\n");
            }
        }
        svg.append("  </g>\n\n");

        // Camada 3: Moveis (Furniture)
        svg.append("  <!-- Camada: Moveis -->\n");
        svg.append("  <g id=\"furniture\">\n");
        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (piece.isVisible()) {
                float[][] points = piece.getPoints();
                if (points != null && points.length >= 3) {
                    StringBuilder pointsStr = new StringBuilder();
                    for (float[] p : points) {
                        if (pointsStr.length() > 0) pointsStr.append(" ");
                        pointsStr.append(DF.format(p[0])).append(",").append(DF.format(p[1]));
                    }
                    svg.append("    <polygon class=\"furniture-polygon\" points=\"").append(pointsStr).append("\" />\n");

                    String name = piece.getName();
                    if (name != null && !name.trim().isEmpty()) {
                        svg.append("    <text class=\"furniture-label\" x=\"").append(DF.format(piece.getX()))
                                .append("\" y=\"").append(DF.format(piece.getY() + 3)).append("\">")
                                .append(escapeXml(name)).append("</text>\n");
                    }
                }
            }
        }
        svg.append("  </g>\n\n");

        // Camada 4: Cotas (Dimension Lines)
        svg.append("  <!-- Camada: Cotas -->\n");
        svg.append("  <g id=\"dimensionLines\">\n");
        for (DimensionLine dim : home.getDimensionLines()) {
            float x1 = dim.getXStart();
            float y1 = dim.getYStart();
            float x2 = dim.getXEnd();
            float y2 = dim.getYEnd();
            float midX = (x1 + x2) / 2.0f;
            float midY = (y1 + y2) / 2.0f;

            svg.append("    <line class=\"dim-line\" x1=\"").append(DF.format(x1))
                    .append("\" y1=\"").append(DF.format(y1))
                    .append("\" x2=\"").append(DF.format(x2))
                    .append("\" y2=\"").append(DF.format(y2)).append("\" />\n");

            float lengthCm = dim.getLength();
            svg.append("    <text class=\"dim-label\" x=\"").append(DF.format(midX))
                    .append("\" y=\"").append(DF.format(midY - 4)).append("\">")
                    .append(DF.format(lengthCm)).append(" cm</text>\n");
        }
        svg.append("  </g>\n\n");

        // Camada 5: Textos e Anotacoes (Labels)
        svg.append("  <!-- Camada: Anotacoes -->\n");
        svg.append("  <g id=\"labels\">\n");
        for (Label label : home.getLabels()) {
            String text = label.getText();
            if (text != null && !text.trim().isEmpty()) {
                svg.append("    <text class=\"annotation-label\" x=\"").append(DF.format(label.getX()))
                        .append("\" y=\"").append(DF.format(label.getY())).append("\">")
                        .append(escapeXml(text)).append("</text>\n");
            }
        }
        svg.append("  </g>\n\n");

        svg.append("</svg>\n");
        return svg.toString();
    }

    /**
     * Grava o SVG diretamente em arquivo.
     */
    public static void exportToFile(Home home, File targetFile) throws IOException {
        String svgContent = exportToSvg(home);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(svgContent);
        }
    }

    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
