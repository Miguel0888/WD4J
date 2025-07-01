package de.bund.zrb.ui.commands;

import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;
import java.util.List;

public class UserSelectionCommand extends ShortcutMenuCommand {

    private final UserRegistry userRegistry;

    public UserSelectionCommand(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public String getId() {
        return "users.select";
    }

    @Override
    public String getLabel() {
        return "Benutzer wählen...";
    }

    @Override
    public void perform() {
        List<UserRegistry.User> users = userRegistry.getAll();
        String[] options = new String[users.size() + 1];
        options[0] = "<Keinen>"; // Special option

        for (int i = 0; i < users.size(); i++) {
            options[i + 1] = users.get(i).getUsername();
        }

        // Aktuellen Usernamen als Vorauswahl ermitteln
        UserRegistry.User current = UserContextMappingService.getInstance().getCurrentUser();
        String initial = "<Keinen>";
        if (current != null) {
            initial = current.getUsername();
        }

        String selection = (String) JOptionPane.showInputDialog(
                null,
                "Benutzer auswählen:",
                "Benutzer wählen",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                initial // Jetzt korrekt!
        );

        if (selection == null) {
            return; // Dialog abgebrochen
        }

        if ("<Keinen>".equals(selection)) {
            UserContextMappingService.getInstance().setCurrentUser(null);
        } else {
            UserRegistry.User selectedUser = users.stream()
                    .filter(u -> u.getUsername().equals(selection))
                    .findFirst()
                    .orElse(null);

            UserContextMappingService.getInstance().setCurrentUser(selectedUser);
        }
    }

}
