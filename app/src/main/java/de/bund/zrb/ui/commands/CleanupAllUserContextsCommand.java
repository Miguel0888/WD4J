package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;

/** Debug-Tool: schließt alle UserContexts remote (außer default), um aufzuräumen. */
public class CleanupAllUserContextsCommand extends ShortcutMenuCommand {
    @Override
    public String getId() {
        return "debug.cleanupUserContexts";
    }

    @Override
    public String getLabel() {
        return "Alle UserContexts bereinigen";
    }

    @Override
    public void perform() {
        int res1 = JOptionPane.showConfirmDialog(
                null,
                "Wirklich alle UserContexts (außer 'default') schließen?",
                "Bereinigung bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (res1 != JOptionPane.YES_OPTION) return;

        int res2 = JOptionPane.showConfirmDialog(
                null,
                "Achtung: Diese Aktion kann nicht rückgängig gemacht werden.\n" +
                        "Offene Sitzungen/Logins werden beendet. Fortfahren?",
                "Letzte Bestätigung",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (res2 != JOptionPane.YES_OPTION) return;

        BrowserServiceImpl.getInstance().closeAllUserContexts();
        JOptionPane.showMessageDialog(
                null,
                "Alle UserContexts wurden (soweit möglich) geschlossen.",
                "Bereinigung abgeschlossen",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
