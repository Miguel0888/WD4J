package de.bund.zrb.ui.commands;

import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.MainWindow;

public class ToggleRightDrawerCommand extends ShortcutMenuCommand {

    private final MainWindow mainWindow;

    public ToggleRightDrawerCommand(MainWindow window) {
        this.mainWindow = window;
    }

    @Override
    public String getId() { return "view.toggleRightDrawer"; }

    @Override
    public String getLabel() { return "Rechte Seitenleiste ein-/ausblenden"; }

    @Override
    public void perform() {
        mainWindow.toggleRightDrawer();
    }
}
