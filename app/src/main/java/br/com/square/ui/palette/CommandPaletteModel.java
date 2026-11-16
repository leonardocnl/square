package br.com.square.ui.palette;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.IconManager;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeView;
import com.eteks.sweethome3d.viewcontroller.HomeView.ActionType;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modelo de dados e motor de busca para a Paleta de Comandos.
 * Indexa exclusivamente acoes oficiais traduzidas do Sweet Home 3D e pecas do catalogo,
 * filtrando qualquer acao interna do Swing.
 */
public class CommandPaletteModel {

    private final Home home;
    private final HomeController controller;
    private final UserPreferences preferences;
    private final ActionMap actionMap;

    private final List<PaletteItem> indexedItems = new ArrayList<PaletteItem>();

    // Dicionario de nomes em portugues e categorias para ActionType do Sweet Home 3D
    private static final Map<ActionType, String> ACTION_PT_NAMES = new EnumMap<ActionType, String>(ActionType.class);
    private static final Map<ActionType, String> ACTION_CATEGORIES = new EnumMap<ActionType, String>(ActionType.class);

    static {
        // Arquivo
        registerAction(ActionType.NEW_HOME, "Novo projeto", "Arquivo");
        registerAction(ActionType.NEW_HOME_FROM_EXAMPLE, "Novo projeto a partir de exemplo...", "Arquivo");
        registerAction(ActionType.OPEN, "Abrir projeto...", "Arquivo");
        registerAction(ActionType.SAVE, "Salvar projeto", "Arquivo");
        registerAction(ActionType.SAVE_AS, "Salvar como...", "Arquivo");
        registerAction(ActionType.SAVE_AND_COMPRESS, "Salvar e compactar...", "Arquivo");
        registerAction(ActionType.PAGE_SETUP, "Configurar p\u00E1gina...", "Arquivo");
        registerAction(ActionType.PRINT_PREVIEW, "Visualizar impress\u00E3o...", "Arquivo");
        registerAction(ActionType.PRINT, "Imprimir...", "Arquivo");
        registerAction(ActionType.PRINT_TO_PDF, "Exportar para PDF...", "Arquivo");
        registerAction(ActionType.PREFERENCES, "Prefer\u00EAncias...", "Arquivo");
        registerAction(ActionType.CLOSE, "Fechar projeto", "Arquivo");
        registerAction(ActionType.EXIT, "Sair do Square", "Arquivo");

        // Edicao
        registerAction(ActionType.UNDO, "Desfazer", "Edi\u00E7\u00E3o");
        registerAction(ActionType.REDO, "Refazer", "Edi\u00E7\u00E3o");
        registerAction(ActionType.CUT, "Recortar", "Edi\u00E7\u00E3o");
        registerAction(ActionType.COPY, "Copiar", "Edi\u00E7\u00E3o");
        registerAction(ActionType.PASTE, "Colar", "Edi\u00E7\u00E3o");
        registerAction(ActionType.PASTE_TO_GROUP, "Colar no grupo", "Edi\u00E7\u00E3o");
        registerAction(ActionType.PASTE_STYLE, "Colar estilo", "Edi\u00E7\u00E3o");
        registerAction(ActionType.DELETE, "Excluir", "Edi\u00E7\u00E3o");
        registerAction(ActionType.SELECT_ALL, "Selecionar tudo", "Edi\u00E7\u00E3o");

        // Ferramentas da Planta 2D
        registerAction(ActionType.SELECT, "Selecionar elementos", "Ferramentas");
        registerAction(ActionType.PAN, "Mover visualiza\u00E7\u00E3o (Pan)", "Ferramentas");
        registerAction(ActionType.CREATE_WALLS, "Criar paredes", "Ferramentas");
        registerAction(ActionType.CREATE_ROOMS, "Criar c\u00F4modos", "Ferramentas");
        registerAction(ActionType.CREATE_POLYLINES, "Criar polilinhas", "Ferramentas");
        registerAction(ActionType.CREATE_DIMENSION_LINES, "Criar linhas de cota", "Ferramentas");
        registerAction(ActionType.CREATE_LABELS, "Adicionar textos e r\u00F3tulos", "Ferramentas");
        registerAction(ActionType.LOCK_BASE_PLAN, "Travar plano de fundo", "Ferramentas");
        registerAction(ActionType.UNLOCK_BASE_PLAN, "Destravar plano de fundo", "Ferramentas");

        // Mobiliario
        registerAction(ActionType.ADD_HOME_FURNITURE, "Adicionar mob\u00EDlia selecionada \u00E0 planta", "Mob\u00EDlia");
        registerAction(ActionType.MODIFY_FURNITURE, "Modificar mob\u00EDlia selecionada...", "Mob\u00EDlia");
        registerAction(ActionType.DELETE_HOME_FURNITURE, "Excluir mob\u00EDlia selecionada", "Mob\u00EDlia");
        registerAction(ActionType.GROUP_FURNITURE, "Agrupar mob\u00EDlia", "Mob\u00EDlia");
        registerAction(ActionType.UNGROUP_FURNITURE, "Desagrupar mob\u00EDlia", "Mob\u00EDlia");
        registerAction(ActionType.ALIGN_FURNITURE_ON_TOP, "Alinhar mob\u00EDlia no topo", "Mob\u00EDlia");
        registerAction(ActionType.ALIGN_FURNITURE_ON_BOTTOM, "Alinhar mob\u00EDlia na base", "Mob\u00EDlia");
        registerAction(ActionType.ALIGN_FURNITURE_ON_LEFT, "Alinhar mob\u00EDlia \u00E0 esquerda", "Mob\u00EDlia");
        registerAction(ActionType.ALIGN_FURNITURE_ON_RIGHT, "Alinhar mob\u00EDlia \u00E0 direita", "Mob\u00EDlia");
        registerAction(ActionType.IMPORT_FURNITURE, "Importar mob\u00EDlia...", "Mob\u00EDlia");
        registerAction(ActionType.IMPORT_FURNITURE_LIBRARY, "Importar biblioteca de mob\u00EDlia (.sh3f)...", "Mob\u00EDlia");
        registerAction(ActionType.IMPORT_TEXTURE, "Importar textura...", "Mob\u00EDlia");
        registerAction(ActionType.IMPORT_TEXTURES_LIBRARY, "Importar biblioteca de texturas (.sh3t)...", "Mob\u00EDlia");

        // Visualizacao 3D
        registerAction(ActionType.VIEW_FROM_TOP, "Vis\u00E3o a\u00E9rea (Top-Down)", "Visualiza\u00E7\u00E3o 3D");
        registerAction(ActionType.VIEW_FROM_OBSERVER, "Vis\u00E3o do observador virtual", "Visualiza\u00E7\u00E3o 3D");
        registerAction(ActionType.MODIFY_3D_ATTRIBUTES, "Modificar atributos 3D...", "Visualiza\u00E7\u00E3o 3D");
        registerAction(ActionType.CREATE_PHOTO, "Criar foto 3D...", "Visualiza\u00E7\u00E3o 3D");
        registerAction(ActionType.CREATE_VIDEO, "Criar v\u00EDdeo 3D...", "Visualiza\u00E7\u00E3o 3D");
        registerAction(ActionType.EXPORT_TO_OBJ, "Exportar para formato OBJ...", "Visualiza\u00E7\u00E3o 3D");
        registerAction(ActionType.ZOOM_IN, "Aproximar zoom", "Visualiza\u00E7\u00E3o");
        registerAction(ActionType.ZOOM_OUT, "Afastar zoom", "Visualiza\u00E7\u00E3o");
    }

    private static void registerAction(ActionType type, String ptName, String category) {
        ACTION_PT_NAMES.put(type, ptName);
        ACTION_CATEGORIES.put(type, category);
    }

    public CommandPaletteModel(Home home, HomeController controller, UserPreferences preferences, ActionMap actionMap) {
        this.home = home;
        this.controller = controller;
        this.preferences = preferences;
        this.actionMap = actionMap;

        reindex();
    }

    public void reindex() {
        indexedItems.clear();

        // 1. Indexar exclusivamente acoes oficiais do Sweet Home 3D
        if (actionMap != null) {
            for (final ActionType actionType : ActionType.values()) {
                final Action action = actionMap.get(actionType);
                if (action != null) {
                    String actionName = (String) action.getValue(Action.NAME);
                    if (actionName == null || actionName.trim().isEmpty() || actionName.equals(actionType.name())) {
                        actionName = (String) action.getValue(Action.SHORT_DESCRIPTION);
                    }
                    if (actionName == null || actionName.trim().isEmpty() || actionName.equals(actionType.name())) {
                        actionName = ACTION_PT_NAMES.get(actionType);
                    }

                    if (actionName != null && !actionName.trim().isEmpty()) {
                        final String finalName = actionName.trim();
                        // Filtra potenciais acoes internas residuais
                        if (finalName.equalsIgnoreCase("release") || finalName.equalsIgnoreCase("press")) {
                            continue;
                        }

                        Icon icon = null;
                        Object iconObj = action.getValue(Action.SMALL_ICON);
                        if (iconObj instanceof Icon) {
                            icon = (Icon) iconObj;
                        }

                        String category = ACTION_CATEGORIES.get(actionType);
                        if (category == null) {
                            category = "Comando";
                        }

                        indexedItems.add(new PaletteItem(
                                finalName,
                                category,
                                PaletteItem.ItemType.COMMAND,
                                icon,
                                new Runnable() {
                                    public void run() {
                                        action.actionPerformed(new ActionEvent(
                                                this, ActionEvent.ACTION_PERFORMED, finalName));
                                    }
                                }
                        ));
                    }
                }
            }
        }

        // Acoes estendidas de exportacao do Square
        if (home != null) {
            indexedItems.add(new PaletteItem(
                    "Exportar planta em SVG...",
                    "Exporta\u00E7\u00E3o",
                    PaletteItem.ItemType.COMMAND,
                    null,
                    new Runnable() {
                        public void run() {
                            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                            fileChooser.setDialogTitle("Exportar Planta em SVG");
                            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Scalable Vector Graphics (*.svg)", "svg"));
                            fileChooser.setSelectedFile(new java.io.File("planta-arquitetural.svg"));
                            int result = fileChooser.showSaveDialog(null);
                            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                java.io.File selectedFile = fileChooser.getSelectedFile();
                                if (!selectedFile.getName().toLowerCase().endsWith(".svg")) {
                                    selectedFile = new java.io.File(selectedFile.getAbsolutePath() + ".svg");
                                }
                                try {
                                    br.com.square.export.SvgPlanExporter.exportToFile(home, selectedFile);
                                    javax.swing.JOptionPane.showMessageDialog(null, "Planta vetorial salva com sucesso em:\n" + selectedFile.getAbsolutePath(),
                                            "Exportacao SVG Concluida", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                                } catch (Exception ex) {
                                    javax.swing.JOptionPane.showMessageDialog(null, "Falha ao exportar SVG: " + ex.getMessage(),
                                            "Erro de Exportacao", javax.swing.JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
            ));

            indexedItems.add(new PaletteItem(
                    "Memorial Descritivo e Quantitativos (BOM)...",
                    "Relat\u00F3rios",
                    PaletteItem.ItemType.COMMAND,
                    null,
                    new Runnable() {
                        public void run() {
                            br.com.square.ui.report.ProjectSpecificationDialog dialog =
                                    new br.com.square.ui.report.ProjectSpecificationDialog(null, home);
                            dialog.setVisible(true);
                        }
                    }
            ));
        }

        // 2. Indexar Moveis do Catalogo com categoria e dimensoes em cm
        if (preferences != null && preferences.getFurnitureCatalog() != null) {
            for (final FurnitureCategory category : preferences.getFurnitureCatalog().getCategories()) {
                for (final CatalogPieceOfFurniture piece : category.getFurniture()) {
                    indexedItems.add(new PaletteItem(
                            piece.getName(),
                            category.getName() + " \u2022 " + Math.round(piece.getWidth()) + "x"
                                    + Math.round(piece.getDepth()) + "x" + Math.round(piece.getHeight()) + " cm",
                            PaletteItem.ItemType.FURNITURE,
                            IconManager.getInstance().getIcon(piece.getIcon(), 24, null),
                            new Runnable() {
                                public void run() {
                                    addFurnitureToHome(piece);
                                }
                            }
                    ));
                }
            }
        }
    }

    private void addFurnitureToHome(CatalogPieceOfFurniture piece) {
        if (controller != null) {
            controller.getFurnitureCatalogController().setSelectedFurniture(Collections.singletonList(piece));
            controller.addHomeFurniture();
        }
    }

    public List<PaletteItem> search(String query) {
        return filter(query);
    }

    public List<PaletteItem> filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<PaletteItem>(indexedItems);
        }

        String normalizedQuery = normalize(query);
        String[] queryTokens = normalizedQuery.split("\\s+");

        List<PaletteItem> exactMatches = new ArrayList<PaletteItem>();
        List<PaletteItem> prefixMatches = new ArrayList<PaletteItem>();
        List<PaletteItem> containsMatches = new ArrayList<PaletteItem>();

        for (PaletteItem item : indexedItems) {
            String normalizedTitle = normalize(item.getTitle());
            String normalizedSubtitle = normalize(item.getSubtitle());

            boolean allTokensMatch = true;
            for (String token : queryTokens) {
                if (!normalizedTitle.contains(token) && !normalizedSubtitle.contains(token)) {
                    allTokensMatch = false;
                    break;
                }
            }

            if (allTokensMatch) {
                if (normalizedTitle.equals(normalizedQuery)) {
                    exactMatches.add(item);
                } else if (normalizedTitle.startsWith(normalizedQuery)) {
                    prefixMatches.add(item);
                } else {
                    containsMatches.add(item);
                }
            }
        }

        List<PaletteItem> result = new ArrayList<PaletteItem>();
        result.addAll(exactMatches);
        result.addAll(prefixMatches);
        result.addAll(containsMatches);
        return result;
    }

    private String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.US).trim();
    }

    public List<PaletteItem> getAllItems() {
        return Collections.unmodifiableList(indexedItems);
    }

    public int getIndexedCount() {
        return indexedItems.size();
    }
}
