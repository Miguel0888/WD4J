package de.bund.zrb.ui.status;

import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Event-free StatusBar manager.
 * - Do not register listeners (no ActionListener, no PropertyChangeListener).
 * - Update only UI on explicit method calls.
 * - Keep all Swing mutations on the EDT.
 * - Ensure selected user exists in combo model (auto-insert if missing).
 */
public final class StatusBarManager {

    // ----- Singleton -----
    private static final StatusBarManager INSTANCE = new StatusBarManager();
    public static StatusBarManager getInstance() { return INSTANCE; }

    // ----- UI -----
    private final JLabel leftLabel = new JLabel("Bereit");
    private final JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
    private final JPanel auxRightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
    private final JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    private final JComboBox<String> userCombo = new JComboBox<String>();
    private final JLabel userPrefix = new JLabel("User:");
    private final JPanel root = new JPanel(new BorderLayout());

    private StatusBarManager() {
        buildUi();
        loadUsersFromRegistryOnInit();  // Load once; no listeners.
        setSelectedUser("<Keinen>");    // Start consistent.
        setRightText("Bereit");
    }

    // ----- Public API -----

    /** Liefert die fertige Statusbar-Komponente zur Platzierung im SOUTH. */
    public JComponent getComponent() {
        return root;
    }

    /** Setze linken Status-Text (nur UI). */
    public void setMessage(final String msg) {
        runOnEdt(new Runnable() {
            @Override public void run() {
                leftLabel.setText(msg != null ? msg : "");
            }
        });
    }

    /** Setze rechte AUX-Komponente (nur UI). */
    public void setRightComponent(final JComponent comp) {
        runOnEdt(new Runnable() {
            @Override public void run() {
                auxRightBox.removeAll();
                if (comp != null) {
                    auxRightBox.add(comp);
                }
                auxRightBox.revalidate();
                auxRightBox.repaint();
            }
        });
    }

    /** Setze rechten AUX-Text (nur UI). */
    public void setRightText(final String text) {
        setRightComponent(new JLabel(text != null ? text : ""));
    }

    /**
     * Wähle den angezeigten Benutzer (Label + Combo).
     * If the name is not in the combo model, insert it (except when null/blank -> "<Keinen>").
     * No events, no services.
     */
    public void setSelectedUser(final String usernameOrNull) {
        final String name = normalizeName(usernameOrNull);
        runOnEdt(new Runnable() {
            @Override public void run() {
                ensureNamePresentInModel(name);
                userCombo.setSelectedItem(name);
                leftLabel.setText("Aktiver Benutzer: " + name);
            }
        });
    }

    /** Setze die Benutzerliste direkt (optional; ereignisfrei). */
    public void setUsers(final List<String> usernames) {
        final List<String> safe = (usernames != null) ? new ArrayList<String>(usernames) : new ArrayList<String>();
        ensurePlaceholderFirst(safe);
        runOnEdt(new Runnable() {
            @Override public void run() {
                String selected = currentSelectionOrNone();
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(safe.toArray(new String[safe.size()]));
                userCombo.setModel(model);
                if (!contains(model, selected)) {
                    selected = "<Keinen>";
                }
                userCombo.setSelectedItem(selected);
            }
        });
    }

    /** Aktualisiere Benutzerliste einmalig aus der Registry (manuell; ereignisfrei). */
    public void refreshUsersFromRegistry() {
        setUsers(collectUsernamesFromRegistry());
    }

    // ----- Internal UI setup -----

    private void buildUi() {
        root.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 50)));
        rightWrap.setOpaque(false);
        auxRightBox.setOpaque(false);
        userBox.setOpaque(false);

        userCombo.setPrototypeDisplayValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ"); // keep width stable
        userCombo.setFocusable(false);
        userCombo.setEnabled(true); // purely visual, no action hooked

        rightWrap.add(auxRightBox);
        rightWrap.add(new JSeparator(SwingConstants.VERTICAL) {{
            setPreferredSize(new Dimension(6, 18));
        }});
        userBox.add(userPrefix);
        userBox.add(userCombo);
        rightWrap.add(userBox);

        root.add(leftLabel, BorderLayout.WEST);
        root.add(rightWrap, BorderLayout.EAST);
    }

    private void loadUsersFromRegistryOnInit() {
        List<String> names = collectUsernamesFromRegistry();
        ensurePlaceholderFirst(names);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(names.toArray(new String[names.size()]));
        userCombo.setModel(model);
        userCombo.setSelectedItem("<Keinen>");
    }

    // ----- Helpers -----

    /** Ensure that "name" exists in model; insert if missing. */
    private void ensureNamePresentInModel(String name) {
        ComboBoxModel<String> model = userCombo.getModel();
        if (model == null) {
            List<String> base = new ArrayList<String>();
            base.add("<Keinen>");
            if (!"<Keinen>".equals(name)) {
                base.add(name);
            }
            userCombo.setModel(new DefaultComboBoxModel<String>(base.toArray(new String[base.size()])));
            return;
        }
        if (!contains(model, name)) {
            // Insert name preserving "<Keinen>" at index 0.
            List<String> values = new ArrayList<String>();
            boolean hasNone = false;
            for (int i = 0; i < model.getSize(); i++) {
                String v = model.getElementAt(i);
                if ("<Keinen>".equals(v)) {
                    hasNone = true;
                }
                values.add(v);
            }
            if (!hasNone) {
                values.add(0, "<Keinen>");
            }
            if (!"<Keinen>".equals(name)) {
                values.add(name);
            }
            userCombo.setModel(new DefaultComboBoxModel<String>(values.toArray(new String[values.size()])));
        }
    }

    private static boolean contains(ComboBoxModel<String> model, String value) {
        if (model == null || value == null) return false;
        for (int i = 0; i < model.getSize(); i++) {
            String v = model.getElementAt(i);
            if (value.equals(v)) return true;
        }
        return false;
    }

    private static void ensurePlaceholderFirst(List<String> names) {
        if (names == null) return;
        boolean has = false;
        for (String n : names) {
            if ("<Keinen>".equals(n)) { has = true; break; }
        }
        if (!has) {
            names.add(0, "<Keinen>");
        } else {
            // Move to front if not already first
            if (!names.isEmpty() && !"<Keinen>".equals(names.get(0))) {
                List<String> copy = new ArrayList<String>(names);
                names.clear();
                names.add("<Keinen>");
                for (int i = 0; i < copy.size(); i++) {
                    String v = copy.get(i);
                    if (!"<Keinen>".equals(v)) {
                        names.add(v);
                    }
                }
            }
        }
    }

    private static String normalizeName(String n) {
        if (n == null) return "<Keinen>";
        String t = n.trim();
        return t.isEmpty() ? "<Keinen>" : t;
    }

    private static List<String> collectUsernamesFromRegistry() {
        List<String> names = new ArrayList<String>();
        names.add("<Keinen>");
        List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (all != null) {
            for (UserRegistry.User u : all) {
                if (u != null && u.getUsername() != null) {
                    names.add(u.getUsername());
                }
            }
        }
        return names;
    }

    private String currentSelectionOrNone() {
        Object sel = userCombo.getSelectedItem();
        return sel == null ? "<Keinen>" : sel.toString();
    }

    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
}
