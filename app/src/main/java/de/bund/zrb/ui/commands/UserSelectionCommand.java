package de.bund.zrb.ui.commands;

import de.bund.zrb.service.SettingsService;
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
        options[0] = "<Keinen>";

        for (int i = 0; i < users.size(); i++) {
            options[i + 1] = users.get(i).getUsername();
        }

        String defaultUser = SettingsService.getInstance().get("defaultUser", String.class);
        String selected = "<Keinen>";

        if (defaultUser != null && !defaultUser.trim().isEmpty()) {
            for (String option : options) {
                if (option.equals(defaultUser)) {
                    selected = defaultUser;
                    break;
                }
            }
        }

        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setSelectedItem(selected);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Benutzer auswählen:"));
        panel.add(comboBox);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Benutzer wählen",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String selection = (String) comboBox.getSelectedItem();
            if (selection == null) {
                return;
            }

            if ("<Keinen>".equals(selection)) {
                UserContextMappingService.getInstance().setCurrentUser(null);
                SettingsService.getInstance().set("defaultUser", null);
            } else {
                UserRegistry.User selectedUser = users.stream()
                        .filter(u -> u.getUsername().equals(selection))
                        .findFirst()
                        .orElse(null);

                UserContextMappingService.getInstance().setCurrentUser(selectedUser);
                SettingsService.getInstance().set("defaultUser", selectedUser.getUsername());
            }
        }
    }



}
