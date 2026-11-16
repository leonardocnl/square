package br.com.square.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Gerenciador global de temas e Look & Feel moderno (FlatLaf) para o Square.
 * Suporta alternancia dinamica entre tema Claro e Escuro e fornece tokens
 * de cores semanticos para desenho da planta 2D.
 */
public final class ThemeManager {
    public enum ThemeMode {
        LIGHT,
        DARK
    }

    public interface ThemeChangeListener {
        void themeChanged(ThemeMode newMode, boolean isDark);
    }

    private static final String PREF_THEME_KEY = "square.theme.mode";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static final List<ThemeChangeListener> listeners = new ArrayList<ThemeChangeListener>();
    private static ThemeMode currentMode = ThemeMode.DARK;

    // Tokens de cores semanticos para a Planta 2D (Modo Claro)
    private static final Color LIGHT_PLAN_BACKGROUND = new Color(0xFF, 0xFF, 0xFF);
    private static final Color LIGHT_PLAN_FOREGROUND = new Color(0x1F, 0x29, 0x37);
    private static final Color LIGHT_PLAN_GRID = new Color(0xE5, 0xE7, 0xEB);
    private static final Color LIGHT_PLAN_RULER_BG = new Color(0xF3, 0xF4, 0xF6);
    private static final Color LIGHT_PLAN_RULER_FG = new Color(0x6B, 0x72, 0x80);
    private static final Color LIGHT_PLAN_SELECTION = new Color(0x25, 0x63, 0xEB);
    private static final Color LIGHT_PLAN_WALL_OUTLINE = new Color(0x11, 0x18, 0x27);

    // Tokens de cores semanticos para a Planta 2D (Modo Escuro contemporaneo)
    private static final Color DARK_PLAN_BACKGROUND = new Color(0x18, 0x18, 0x1B);
    private static final Color DARK_PLAN_FOREGROUND = new Color(0xF4, 0xF4, 0xF5);
    private static final Color DARK_PLAN_GRID = new Color(0x27, 0x27, 0x2A);
    private static final Color DARK_PLAN_RULER_BG = new Color(0x20, 0x20, 0x24);
    private static final Color DARK_PLAN_RULER_FG = new Color(0xA1, 0xA1, 0xAA);
    private static final Color DARK_PLAN_SELECTION = new Color(0x3B, 0x82, 0xF6);
    private static final Color DARK_PLAN_WALL_OUTLINE = new Color(0xFA, 0xFA, 0xFA);

    private ThemeManager() {
    }

    /**
     * Inicializa o Look & Feel moderno durante a inicializacao da aplicacao.
     */
    public static void setup() {
        String saved = prefs.get(PREF_THEME_KEY, ThemeMode.DARK.name());
        try {
            currentMode = ThemeMode.valueOf(saved.toUpperCase());
        } catch (IllegalArgumentException e) {
            currentMode = ThemeMode.DARK;
        }

        configureFlatLafDefaults();
        applyLaf(currentMode);
    }

    private static void configureFlatLafDefaults() {
        // Estilizacao minimalista e moderna
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("SplitPaneDivider.gripDotCount", 0);
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Tree.showDefaultIcons", true);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
    }

    private static void applyLaf(ThemeMode mode) {
        if (mode == ThemeMode.DARK) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    public static ThemeMode getCurrentMode() {
        return currentMode;
    }

    public static boolean isDark() {
        return currentMode == ThemeMode.DARK;
    }

    /**
     * Alterna dinamicamente entre tema Claro e Escuro com transicao visual.
     */
    public static void toggleTheme() {
        setTheme(isDark() ? ThemeMode.LIGHT : ThemeMode.DARK);
    }

    /**
     * Define o tema desejado e atualiza todos os componentes da aplicacao.
     */
    public static void setTheme(ThemeMode mode) {
        if (mode == null) {
            mode = ThemeMode.DARK;
        }
        currentMode = mode;
        prefs.put(PREF_THEME_KEY, mode.name());

        FlatAnimatedLafChange.showSnapshot();
        applyLaf(mode);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();

        notifyListeners();
    }

    public static void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        boolean dark = isDark();
        for (ThemeChangeListener listener : listeners) {
            listener.themeChanged(currentMode, dark);
        }
    }

    // --- Tokens Semanticos para o Canvas da Planta 2D ---

    public static Color getPlanBackground() {
        return isDark() ? DARK_PLAN_BACKGROUND : LIGHT_PLAN_BACKGROUND;
    }

    public static Color getPlanForeground() {
        return isDark() ? DARK_PLAN_FOREGROUND : LIGHT_PLAN_FOREGROUND;
    }

    public static Color getPlanGridColor() {
        return isDark() ? DARK_PLAN_GRID : LIGHT_PLAN_GRID;
    }

    public static Color getPlanRulerBackground() {
        return isDark() ? DARK_PLAN_RULER_BG : LIGHT_PLAN_RULER_BG;
    }

    public static Color getPlanRulerForeground() {
        return isDark() ? DARK_PLAN_RULER_FG : LIGHT_PLAN_RULER_FG;
    }

    public static Color getPlanSelectionColor() {
        return isDark() ? DARK_PLAN_SELECTION : LIGHT_PLAN_SELECTION;
    }

    public static Color getPlanWallOutlineColor() {
        return isDark() ? DARK_PLAN_WALL_OUTLINE : LIGHT_PLAN_WALL_OUTLINE;
    }
}

