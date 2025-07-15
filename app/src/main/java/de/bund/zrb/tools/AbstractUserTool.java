package de.bund.zrb.tools;

import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;

public abstract class AbstractUserTool {

    /**
     * Liefert den aktuellen Benutzer aus dem aktiven Browserkontext.
     *
     * @return aktueller Benutzer
     * @throws IllegalStateException wenn kein Benutzer zugeordnet ist
     */
    protected UserRegistry.User getCurrentUserOrFail() {
        UserRegistry.User user = UserContextMappingService.getInstance().getCurrentUser();

        if (user == null) {
            throw new IllegalStateException("Kein Benutzer für aktiven Kontext gefunden.");
        }

        return user;
    }
}
