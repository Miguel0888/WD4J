package de.bund.zrb.ui.widgets;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.util.List;

/** Kleine Kombobox für die User-Auswahl (mit <Keinen>). */
public final class UserSelectionCombo extends JPanel {
    private final JComboBox<String> combo = new JComboBox<>();
    private final UserRegistry userRegistry;

    public UserSelectionCombo(UserRegistry registry) {
        setOpaque(false);
        this.userRegistry = registry;

        combo.setFocusable(false);
        add(combo);

        rebuildModel();
        selectDefaultFromSettings();

        combo.addActionListener(e -> {
            Object sel = combo.getSelectedItem();
            if (sel == null || "<Keinen>".equals(sel)) {
                UserContextMappingService.getInstance().setCurrentUser(null);
                SettingsService.getInstance().set("defaultUser", null);
            } else {
                String username = sel.toString();
                UserRegistry.User u = userRegistry.getAll().stream()
                        .filter(it -> username.equals(it.getUsername()))
                        .findFirst().orElse(null);
                UserContextMappingService.getInstance().setCurrentUser(u);
                SettingsService.getInstance().set("defaultUser", username);
            }
        });
    }

    /** Model neu aufbauen (falls Registry geändert wurde). */
    public void rebuildModel() {
        String keep = (String) combo.getSelectedItem();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("<Keinen>");
        List<UserRegistry.User> users = userRegistry.getAll();
        for (UserRegistry.User u : users) if (u != null && u.getUsername() != null) model.addElement(u.getUsername());
        combo.setModel(model);
        if (keep != null) combo.setSelectedItem(keep);
    }

    public String getSelectedUsername() {
        Object v = combo.getSelectedItem();
        return (v == null || "<Keinen>".equals(v)) ? null : v.toString();
    }

    private void selectDefaultFromSettings() {
        String def = SettingsService.getInstance().get("defaultUser", String.class);
        combo.setSelectedItem((def == null || def.isEmpty()) ? "<Keinen>" : def);
    }
}
