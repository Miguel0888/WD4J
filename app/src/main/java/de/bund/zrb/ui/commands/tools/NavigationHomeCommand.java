package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.service.ToolsRegistry;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class NavigationHomeCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "browser.tab.home";
    }

    @Override
    public String getLabel() {
        return "Startseite";
    }

    @Override
    public void perform() {
        UserRegistry.User currentUser = UserContextMappingService.getInstance().getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("Kein Benutzer ausgewählt. Bitte wähle einen Benutzer mit Startseite.");
        }

        ToolsRegistry.getInstance().navigationTool().navigateToStartPage();
    }
}
