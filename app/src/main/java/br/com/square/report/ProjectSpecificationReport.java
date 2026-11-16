package br.com.square.report;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gerador de Memorial Descritivo e Relatorio Quantitativo Executivo do Projeto (BOM).
 * Agrupa dados arquiteturais, comodos, paredes, acabamentos e quantitativo de mobiliario
 * por comodo para geracao de orcamentos e especificacoes executivas.
 */
public class ProjectSpecificationReport {

    private static final DecimalFormat METRIC_DF;
    private static final DecimalFormat CURRENCY_DF;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        METRIC_DF = new DecimalFormat("0.00", symbols);
        CURRENCY_DF = new DecimalFormat("#,##0.00", symbols);
    }

    public static class RoomSpec {
        private final String name;
        private final double areaM2;
        private final double perimeterM;
        private final String floorFinish;
        private final String ceilingFinish;
        private final List<FurnitureSpec> items = new ArrayList<>();

        public RoomSpec(String name, double areaM2, double perimeterM, String floorFinish, String ceilingFinish) {
            this.name = name;
            this.areaM2 = areaM2;
            this.perimeterM = perimeterM;
            this.floorFinish = floorFinish;
            this.ceilingFinish = ceilingFinish;
        }

        public String getName() { return name; }
        public double getAreaM2() { return areaM2; }
        public double getPerimeterM() { return perimeterM; }
        public String getFloorFinish() { return floorFinish; }
        public String getCeilingFinish() { return ceilingFinish; }
        public List<FurnitureSpec> getItems() { return items; }
    }

    public static class FurnitureSpec {
        private final String name;
        private final String roomName;
        private final float widthCm;
        private final float depthCm;
        private final float heightCm;
        private final float elevationCm;
        private final boolean doorOrWindow;
        private int quantity;
        private BigDecimal unitPrice;

        public FurnitureSpec(String name, String roomName, float widthCm, float depthCm,
                             float heightCm, float elevationCm, boolean doorOrWindow, BigDecimal unitPrice) {
            this.name = name;
            this.roomName = roomName;
            this.widthCm = widthCm;
            this.depthCm = depthCm;
            this.heightCm = heightCm;
            this.elevationCm = elevationCm;
            this.doorOrWindow = doorOrWindow;
            this.quantity = 1;
            this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        }

        public String getName() { return name; }
        public String getRoomName() { return roomName; }
        public float getWidthCm() { return widthCm; }
        public float getDepthCm() { return depthCm; }
        public float getHeightCm() { return heightCm; }
        public float getElevationCm() { return elevationCm; }
        public boolean isDoorOrWindow() { return doorOrWindow; }
        public int getQuantity() { return quantity; }
        public void incrementQuantity() { this.quantity++; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalPrice() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    public static class ReportData {
        private double totalAreaM2 = 0;
        private double totalWallLengthM = 0;
        private double totalWallSurfaceAreaM2 = 0;
        private int totalFurnitureCount = 0;
        private int totalDoorsAndWindowsCount = 0;
        private BigDecimal totalCost = BigDecimal.ZERO;

        private final List<RoomSpec> rooms = new ArrayList<>();
        private final List<FurnitureSpec> furnitureList = new ArrayList<>();
        private final List<FurnitureSpec> doorsAndWindowsList = new ArrayList<>();

        public double getTotalAreaM2() { return totalAreaM2; }
        public double getTotalWallLengthM() { return totalWallLengthM; }
        public double getTotalWallSurfaceAreaM2() { return totalWallSurfaceAreaM2; }
        public int getTotalFurnitureCount() { return totalFurnitureCount; }
        public int getTotalDoorsAndWindowsCount() { return totalDoorsAndWindowsCount; }
        public BigDecimal getTotalCost() { return totalCost; }
        public List<RoomSpec> getRooms() { return rooms; }
        public List<FurnitureSpec> getFurnitureList() { return furnitureList; }
        public List<FurnitureSpec> getDoorsAndWindowsList() { return doorsAndWindowsList; }
    }

    /**
     * Extrai e compila os dados do memorial descritivo a partir do modelo Home.
     */
    public static ReportData generateReportData(Home home) {
        ReportData data = new ReportData();
        if (home == null) {
            return data;
        }

        // 1. Processamento de Comodos
        List<Room> homeRooms = home.getRooms();
        for (int i = 0; i < homeRooms.size(); i++) {
            Room room = homeRooms.get(i);
            String name = room.getName();
            if (name == null || name.trim().isEmpty()) {
                name = "Comodo " + (i + 1);
            }

            double areaM2 = room.getArea() / 10000.0;
            data.totalAreaM2 += areaM2;

            double perimeterM = calculatePerimeterM(room.getPoints());

            String floorFinish = "Padrao";
            HomeTexture floorTex = room.getFloorTexture();
            if (floorTex != null) {
                floorFinish = floorTex.getName();
            } else if (room.getFloorColor() != null) {
                floorFinish = String.format("#%06X", (room.getFloorColor() & 0xFFFFFF));
            }

            String ceilingFinish = "Padrao";
            HomeTexture ceilTex = room.getCeilingTexture();
            if (ceilTex != null) {
                ceilingFinish = ceilTex.getName();
            } else if (room.getCeilingColor() != null) {
                ceilingFinish = String.format("#%06X", (room.getCeilingColor() & 0xFFFFFF));
            }

            data.rooms.add(new RoomSpec(name, areaM2, perimeterM, floorFinish, ceilingFinish));
        }

        // 2. Processamento de Paredes
        for (Wall wall : home.getWalls()) {
            double lengthM = wall.getLength() / 100.0;
            data.totalWallLengthM += lengthM;

            double heightM = wall.getHeight() != null ? (wall.getHeight() / 100.0) : 2.50;
            // Ambas as faces da parede para estimativa de pintura/revestimento
            data.totalWallSurfaceAreaM2 += (lengthM * heightM * 2.0);
        }

        // 3. Processamento e Atribuicao de Moveis aos Comodos
        Map<String, FurnitureSpec> furnitureGroupMap = new LinkedHashMap<>();
        Map<String, FurnitureSpec> doorsWindowsGroupMap = new LinkedHashMap<>();

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            String roomName = "Area Comum / Nao Atribuido";
            float px = piece.getX();
            float py = piece.getY();

            for (Room room : homeRooms) {
                if (room.containsPoint(px, py, 0.0f)) {
                    roomName = (room.getName() != null && !room.getName().trim().isEmpty())
                            ? room.getName() : "Comodo sem nome";
                    break;
                }
            }

            String pieceName = piece.getName() != null ? piece.getName() : "Item sem nome";
            BigDecimal price = piece.getPrice() != null ? piece.getPrice() : BigDecimal.ZERO;
            data.totalCost = data.totalCost.add(price);

            String groupKey = pieceName + "|" + roomName + "|" + Math.round(piece.getWidth())
                    + "|" + Math.round(piece.getDepth()) + "|" + Math.round(piece.getHeight());

            if (piece.isDoorOrWindow()) {
                data.totalDoorsAndWindowsCount++;
                FurnitureSpec spec = doorsWindowsGroupMap.get(groupKey);
                if (spec != null) {
                    spec.incrementQuantity();
                } else {
                    spec = new FurnitureSpec(pieceName, roomName, piece.getWidth(), piece.getDepth(),
                            piece.getHeight(), piece.getElevation(), true, price);
                    doorsWindowsGroupMap.put(groupKey, spec);
                }
            } else {
                data.totalFurnitureCount++;
                FurnitureSpec spec = furnitureGroupMap.get(groupKey);
                if (spec != null) {
                    spec.incrementQuantity();
                } else {
                    spec = new FurnitureSpec(pieceName, roomName, piece.getWidth(), piece.getDepth(),
                            piece.getHeight(), piece.getElevation(), false, price);
                    furnitureGroupMap.put(groupKey, spec);
                }
            }
        }

        data.furnitureList.addAll(furnitureGroupMap.values());
        data.doorsAndWindowsList.addAll(doorsWindowsGroupMap.values());

        return data;
    }

    private static double calculatePerimeterM(float[][] points) {
        if (points == null || points.length < 2) {
            return 0.0;
        }
        double perimeterCm = 0.0;
        for (int i = 0; i < points.length; i++) {
            float[] p1 = points[i];
            float[] p2 = points[(i + 1) % points.length];
            double dx = p2[0] - p1[0];
            double dy = p2[1] - p1[1];
            perimeterCm += Math.hypot(dx, dy);
        }
        return perimeterCm / 100.0;
    }

    /**
     * Exporta o Memorial Descritivo em formato HTML executivo moderno e responsivo.
     */
    public static String generateHtmlReport(Home home) {
        ReportData data = generateReportData(home);

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <title>Memorial Descritivo e Quantitativos — Square</title>\n");
        sb.append("  <style>\n");
        sb.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 32px; }\n");
        sb.append("    .container { max-width: 1080px; margin: 0 auto; background: #1E293B; border-radius: 12px; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.4); }\n");
        sb.append("    h1 { margin-top: 0; color: #38BDF8; font-size: 26px; border-bottom: 2px solid #334155; padding-bottom: 12px; }\n");
        sb.append("    h2 { color: #94A3B8; font-size: 18px; margin-top: 28px; border-bottom: 1px solid #334155; padding-bottom: 6px; }\n");
        sb.append("    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-top: 20px; }\n");
        sb.append("    .card { background: #0F172A; border-radius: 8px; padding: 18px; border: 1px solid #334155; }\n");
        sb.append("    .card-val { font-size: 24px; font-weight: bold; color: #38BDF8; margin-top: 4px; }\n");
        sb.append("    .card-lbl { font-size: 12px; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        sb.append("    table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 14px; }\n");
        sb.append("    th { background: #0F172A; color: #38BDF8; text-align: left; padding: 10px 14px; border-bottom: 2px solid #334155; }\n");
        sb.append("    td { padding: 10px 14px; border-bottom: 1px solid #334155; }\n");
        sb.append("    tr:hover { background: rgba(56, 189, 248, 0.05); }\n");
        sb.append("    .text-right { text-align: right; }\n");
        sb.append("    .footer { margin-top: 36px; text-align: center; color: #64748B; font-size: 12px; }\n");
        sb.append("    @media print { body { background: #FFFFFF; color: #000000; padding: 0; } .container { box-shadow: none; border: none; } th, .card-val { color: #0284C7; } }\n");
        sb.append("  </style>\n</head>\n<body>\n");
        sb.append("  <div class=\"container\">\n");
        sb.append("    <h1>Memorial Descritivo e Quantitativo Executivo</h1>\n");

        // Cards de Resumo
        sb.append("    <div class=\"cards\">\n");
        sb.append("      <div class=\"card\"><div class=\"card-lbl\">Area Total Util</div><div class=\"card-val\">")
                .append(METRIC_DF.format(data.totalAreaM2)).append(" m&#178;</div></div>\n");
        sb.append("      <div class=\"card\"><div class=\"card-lbl\">Extensao de Paredes</div><div class=\"card-val\">")
                .append(METRIC_DF.format(data.totalWallLengthM)).append(" m</div></div>\n");
        sb.append("      <div class=\"card\"><div class=\"card-lbl\">Area Superficial de Paredes</div><div class=\"card-val\">")
                .append(METRIC_DF.format(data.totalWallSurfaceAreaM2)).append(" m&#178;</div></div>\n");
        sb.append("      <div class=\"card\"><div class=\"card-lbl\">Total de Moveis</div><div class=\"card-val\">")
                .append(data.totalFurnitureCount).append(" un.</div></div>\n");
        sb.append("      <div class=\"card\"><div class=\"card-lbl\">Esquadrias (Portas/Janelas)</div><div class=\"card-val\">")
                .append(data.totalDoorsAndWindowsCount).append(" un.</div></div>\n");
        sb.append("    </div>\n");

        // Tabela de Comodos
        sb.append("    <h2>1. Quadro de Areas e Comodos</h2>\n");
        sb.append("    <table>\n");
        sb.append("      <thead><tr><th>Comodo</th><th class=\"text-right\">Area (m&#178;)</th><th class=\"text-right\">Perimetro (m)</th><th>Acabamento Piso</th><th>Acabamento Teto</th></tr></thead>\n");
        sb.append("      <tbody>\n");
        for (RoomSpec r : data.rooms) {
            sb.append("        <tr><td>").append(escapeHtml(r.name)).append("</td>")
                    .append("<td class=\"text-right\">").append(METRIC_DF.format(r.areaM2)).append("</td>")
                    .append("<td class=\"text-right\">").append(METRIC_DF.format(r.perimeterM)).append("</td>")
                    .append("<td>").append(escapeHtml(r.floorFinish)).append("</td>")
                    .append("<td>").append(escapeHtml(r.ceilingFinish)).append("</td></tr>\n");
        }
        sb.append("      </tbody>\n    </table>\n");

        // Tabela de Mobiliario
        sb.append("    <h2>2. Relacao de Mobiliario e Marcenaria</h2>\n");
        sb.append("    <table>\n");
        sb.append("      <thead><tr><th>Item</th><th>Comodo</th><th class=\"text-right\">Qtd</th><th class=\"text-right\">Dimensoes (LxPxA cm)</th><th class=\"text-right\">Preco Unit.</th><th class=\"text-right\">Preco Total</th></tr></thead>\n");
        sb.append("      <tbody>\n");
        for (FurnitureSpec f : data.furnitureList) {
            sb.append("        <tr><td>").append(escapeHtml(f.name)).append("</td>")
                    .append("<td>").append(escapeHtml(f.roomName)).append("</td>")
                    .append("<td class=\"text-right\">").append(f.quantity).append("</td>")
                    .append("<td class=\"text-right\">").append(Math.round(f.widthCm)).append("x")
                    .append(Math.round(f.depthCm)).append("x").append(Math.round(f.heightCm)).append("</td>")
                    .append("<td class=\"text-right\">R$ ").append(CURRENCY_DF.format(f.unitPrice)).append("</td>")
                    .append("<td class=\"text-right\">R$ ").append(CURRENCY_DF.format(f.getTotalPrice())).append("</td></tr>\n");
        }
        sb.append("      </tbody>\n    </table>\n");

        // Tabela de Esquadrias
        if (!data.doorsAndWindowsList.isEmpty()) {
            sb.append("    <h2>3. Quadro de Esquadrias (Portas e Janelas)</h2>\n");
            sb.append("    <table>\n");
            sb.append("      <thead><tr><th>Descricao</th><th>Comodo</th><th class=\"text-right\">Qtd</th><th class=\"text-right\">Largura (cm)</th><th class=\"text-right\">Altura (cm)</th><th class=\"text-right\">Peitoril (cm)</th></tr></thead>\n");
            sb.append("      <tbody>\n");
            for (FurnitureSpec d : data.doorsAndWindowsList) {
                sb.append("        <tr><td>").append(escapeHtml(d.name)).append("</td>")
                        .append("<td>").append(escapeHtml(d.roomName)).append("</td>")
                        .append("<td class=\"text-right\">").append(d.quantity).append("</td>")
                        .append("<td class=\"text-right\">").append(Math.round(d.widthCm)).append("</td>")
                        .append("<td class=\"text-right\">").append(Math.round(d.heightCm)).append("</td>")
                        .append("<td class=\"text-right\">").append(Math.round(d.elevationCm)).append("</td></tr>\n");
            }
            sb.append("      </tbody>\n    </table>\n");
        }

        sb.append("    <div class=\"footer\">Gerado automaticamente pelo Square Architecture Studio</div>\n");
        sb.append("  </div>\n</body>\n</html>\n");

        return sb.toString();
    }

    /**
     * Exporta o Memorial Descritivo em formato CSV padronizado.
     */
    public static String generateCsvReport(Home home) {
        ReportData data = generateReportData(home);
        StringBuilder sb = new StringBuilder();

        sb.append("TIPO;ITEM;COMODO;QUANTIDADE;DIMENSOES_LXPA_CM;AREA_M2;PERIMETRO_M;PRECO_UNITARIO;PRECO_TOTAL\n");

        for (RoomSpec r : data.rooms) {
            sb.append("COMODO;").append(r.name).append(";-;1;-;")
                    .append(METRIC_DF.format(r.areaM2)).append(";")
                    .append(METRIC_DF.format(r.perimeterM)).append(";-;-\n");
        }

        for (FurnitureSpec f : data.furnitureList) {
            sb.append("MOVEL;").append(f.name).append(";").append(f.roomName).append(";")
                    .append(f.quantity).append(";")
                    .append(Math.round(f.widthCm)).append("x").append(Math.round(f.depthCm)).append("x").append(Math.round(f.heightCm)).append(";-;-;")
                    .append(CURRENCY_DF.format(f.unitPrice)).append(";")
                    .append(CURRENCY_DF.format(f.getTotalPrice())).append("\n");
        }

        for (FurnitureSpec d : data.doorsAndWindowsList) {
            sb.append("ESQUADRIA;").append(d.name).append(";").append(d.roomName).append(";")
                    .append(d.quantity).append(";")
                    .append(Math.round(d.widthCm)).append("x").append(Math.round(d.depthCm)).append("x").append(Math.round(d.heightCm)).append(";-;-;-;-\n");
        }

        return sb.toString();
    }

    /**
     * Grava o relatorio HTML diretamente em arquivo.
     */
    public static void exportHtmlToFile(Home home, File targetFile) throws IOException {
        String html = generateHtmlReport(home);
        writeFile(targetFile, html);
    }

    /**
     * Grava o relatorio CSV diretamente em arquivo.
     */
    public static void exportCsvToFile(Home home, File targetFile) throws IOException {
        String csv = generateCsvReport(home);
        writeFile(targetFile, csv);
    }

    private static void writeFile(File targetFile, String content) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
