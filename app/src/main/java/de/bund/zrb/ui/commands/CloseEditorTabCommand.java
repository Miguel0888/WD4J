package de.bund.zrb.ui.commands;

import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;

/**
 * Schließt in der UI den aktuellen Editor-Tab in der Mitte, aber nur wenn es ein persistenter Tab ist.
 * Der Preview-Tab bleibt unberührt.
 */
public class CloseEditorTabCommand extends ShortcutMenuCommand {

    private final JTabbedPane editorTabs;

    public CloseEditorTabCommand(JTabbedPane editorTabs) {
        this.editorTabs = editorTabs;
    }

    @Override
    public String getId() {
        return "view.closeEditorTab";
    }

    @Override
    public String getLabel() {
        return "Aktiven Tab schließen (UI)";
    }

    @Override
    public void perform() {
        int idx = editorTabs.getSelectedIndex();
        if (idx < 0) return;
        java.awt.Component comp = editorTabs.getComponentAt(idx);

        // Preview-Tab erkennen: in TabManager wird der Preview-Tab ohne ClosableTabHeader betrieben,
        // und persistenten Tabs wird ein ClosableTabHeader gesetzt. Wir schließen daher nur Tabs,
        // die ein TabComponent (Header) besitzen (=> persistent).
        java.awt.Component header = editorTabs.getTabComponentAt(idx);
        if (header == null) {
            // kein Header => vermutlich Preview -> nicht schließen
            return;
        }
        // Entferne Tab
        editorTabs.removeTabAt(idx);
    }
}

