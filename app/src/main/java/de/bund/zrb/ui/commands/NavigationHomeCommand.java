package de.bund.zrb.ui.commands;

import de.bund.zrb.service.BrowserService;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

public class NavigationHomeCommand extends ShortcutMenuCommand {

    private final BrowserService browserService;

    public NavigationHomeCommand(BrowserService browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "navigation.home";
    }

    @Override
    public String getLabel() {
        return "Startseite";
    }

    @Override
    public void perform() {
        UserRegistry.User currentUser = UserContextMappingService.getInstance().getCurrentUser();
        String startPage;

        if (currentUser != null) {
            startPage = currentUser.getStartPage();
            if (startPage == null || startPage.trim().isEmpty()) {
                throw new IllegalStateException("Startseite für Benutzer " + currentUser.getUsername() + " ist nicht gesetzt.");
            }
            browserService.getActivePage(currentUser.getUsername()).navigate(startPage);
        } else {
            // Fallback: Vielleicht eine globale Default-URL oder Abbruch
            throw new IllegalStateException("Kein Benutzer ausgewählt. Bitte wähle einen Benutzer mit Startseite.");
        }
    }
}
