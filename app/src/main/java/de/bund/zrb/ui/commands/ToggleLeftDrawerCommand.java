package de.bund.zrb.ui.commands;

import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.MainWindow;

public class ToggleLeftDrawerCommand extends ShortcutMenuCommand {

    private final MainWindow mainWindow;

    public ToggleLeftDrawerCommand(MainWindow window) {
        this.mainWindow = window;
    }

    @Override
    public String getId() { return "view.toggleLeftDrawer"; }

    @Override
    public String getLabel() { return "Linke Seitenleiste ein-/ausblenden"; }

    @Override
    public void perform() {
        mainWindow.toggleLeftDrawer();
    }
}
