package br.com.square.ui.palette;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.IconManager;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Modelo de dados e motor de busca para a Paleta de Comandos.
 * Indexa acoes e itens de catalogo sem acoplamento direto a dialogs de janela.
 */
public class CommandPaletteModel {

    private final Home home;
    private final HomeController controller;
    private final UserPreferences preferences;
    private final ActionMap actionMap;

    private final List<PaletteItem> indexedItems = new ArrayList<PaletteItem>();

    public CommandPaletteModel(Home home, HomeController controller, UserPreferences preferences, ActionMap actionMap) {
        this.home = home;
        this.controller = controller;
        this.preferences = preferences;
        this.actionMap = actionMap;

        reindex();
    }

    public void reindex() {
        indexedItems.clear();

        // 1. Indexar Acoes e Ferramentas do ActionMap
        if (actionMap != null) {
            for (Object key : actionMap.allKeys()) {
                final Action action = actionMap.get(key);
                if (action != null) {
                    Object nameObj = action.getValue(Action.NAME);
                    if (nameObj instanceof String && !((String) nameObj).trim().isEmpty()) {
                        final String actionName = ((String) nameObj).trim();
                        Icon icon = null;
                        Object iconObj = action.getValue(Action.SMALL_ICON);
                        if (iconObj instanceof Icon) {
                            icon = (Icon) iconObj;
                        }

                        indexedItems.add(new PaletteItem(
                                actionName,
                                "Comando",
                                PaletteItem.ItemType.COMMAND,
                                icon,
                                new Runnable() {
                                    public void run() {
                                        action.actionPerformed(new ActionEvent(
                                                this, ActionEvent.ACTION_PERFORMED, actionName));
                                    }
                                }
                        ));
                    }
                }
            }
        }

        // 2. Indexar Moveis do Catalogo
        if (preferences != null && preferences.getFurnitureCatalog() != null) {
            for (final FurnitureCategory category : preferences.getFurnitureCatalog().getCategories()) {
                for (final CatalogPieceOfFurniture piece : category.getFurniture()) {
                    indexedItems.add(new PaletteItem(
                            piece.getName(),
                            category.getName() + " • " + Math.round(piece.getWidth()) + "x"
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

    public List<PaletteItem> filter(String query) {
        List<PaletteItem> results = new ArrayList<PaletteItem>();
        String normalizedQuery = normalize(query);

        for (PaletteItem item : indexedItems) {
            if (normalizedQuery.isEmpty() || normalize(item.getTitle()).contains(normalizedQuery)
                    || normalize(item.getSubtitle()).contains(normalizedQuery)) {
                results.add(item);
                if (results.size() >= 50) {
                    break;
                }
            }
        }
        return results;
    }

    public List<PaletteItem> getAllItems() {
        return Collections.unmodifiableList(indexedItems);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}

