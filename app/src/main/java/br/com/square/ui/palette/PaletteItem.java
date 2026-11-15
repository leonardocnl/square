package br.com.square.ui.palette;

import javax.swing.Icon;

/**
 * Representa um item exibido na Paleta de Comandos (ação do sistema ou móvel do catálogo).
 */
public class PaletteItem {

    public enum ItemType {
        COMMAND,
        FURNITURE
    }

    private final String title;
    private final String subtitle;
    private final ItemType type;
    private final Icon icon;
    private final Runnable executionTask;

    public PaletteItem(String title, String subtitle, ItemType type, Icon icon, Runnable executionTask) {
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
        this.icon = icon;
        this.executionTask = executionTask;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public ItemType getType() {
        return type;
    }

    public Icon getIcon() {
        return icon;
    }

    public void execute() {
        if (executionTask != null) {
            executionTask.run();
        }
    }

    @Override
    public String toString() {
        return title;
    }
}
