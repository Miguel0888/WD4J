// File: app/src/main/java/de/bund/zrb/ui/widgets/UserSelectionCombo.java
package de.bund.zrb.ui.widgets;

import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public final class UserSelectionCombo extends JPanel {

    private final JComboBox<String> combo = new JComboBox<>();
    private final UserRegistry registry;
    private final UserContextMappingService mapping = UserContextMappingService.getInstance();

    // Reentrancy-Flags: vermeiden Ping-Pong zwischen Combo-Action und Service-Event
    private volatile boolean updatingFromService = false;
    private volatile boolean updatingFromCombo   = false;

    private PropertyChangeListener mappingListener;

    public UserSelectionCombo(UserRegistry registry) {
        super(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        this.registry = registry;

        add(new JLabel("User:"));
        combo.setPrototypeDisplayValue("ABCDEFGHIJKLMNOPQRSTUVWX"); // stabile Breite
        combo.setFocusable(false);
        add(combo);

        // Initiale Liste + Auswahl
        rebuildModel();
        selectCurrentFromService();

        // Combo -> Service
        combo.addActionListener(this::onComboChanged);

        // Service -> Combo (nutzt dein PropertyChangeSupport in UserContextMappingService)
        mappingListener = evt -> {
            if (!"currentUser".equals(evt.getPropertyName())) return;
            if (updatingFromCombo) return;
            updatingFromService = true;
            try {
                selectCurrentFromService();
            } finally {
                updatingFromService = false;
            }
        };
        mapping.addPropertyChangeListener(mappingListener);
    }

    private void onComboChanged(ActionEvent e) {
        if (updatingFromService) return;
        updatingFromCombo = true;
        try {
            String sel = (String) combo.getSelectedItem();
            if (sel == null || "<Keinen>".equals(sel)) {
                mapping.setCurrentUser(null);
            } else {
                mapping.setCurrentUser(findByName(sel));
            }
        } finally {
            updatingFromCombo = false;
        }
    }

    /** Baut das Modell stumpf aus der Registry neu auf (inkl. "<Keinen>"). */
    private void rebuildModel() {
        List<String> names = new ArrayList<>();
        names.add("<Keinen>");
        for (UserRegistry.User u : safe(registry.getAll())) {
            names.add(u.getUsername());
        }
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(names.toArray(new String[0]));
        combo.setModel(model);
    }

    /** Setzt die Auswahl anhand des Service-Zustands. */
    private void selectCurrentFromService() {
        String current = mapping.getCurrentUsernameOrNull();
        String want = (current == null) ? "<Keinen>" : current;

        // Falls der Benutzer neu angelegt wurde, ggf. Liste anreichern
        DefaultComboBoxModel<String> m = (DefaultComboBoxModel<String>) combo.getModel();
        boolean found = false;
        for (int i = 0; i < m.getSize(); i++) if (want.equals(m.getElementAt(i))) { found = true; break; }
        if (!found) m.addElement(want);

        combo.setSelectedItem(want);
        combo.revalidate(); combo.repaint();
    }

    private static List<UserRegistry.User> safe(List<UserRegistry.User> in) {
        return (in == null) ? new ArrayList<>() : new ArrayList<>(in);
    }

    private UserRegistry.User findByName(String name) {
        if (name == null) return null;
        for (UserRegistry.User u : safe(registry.getAll())) {
            if (name.equals(u.getUsername())) return u;
        }
        return null;
    }
}
