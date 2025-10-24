// File: app/src/main/java/de/bund/zrb/ui/status/StatusBarManager.java
package de.bund.zrb.ui.status;

import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Event-free StatusBar manager.
 * - Keine Listener/Events – reine UI-Updates über Methoden.
 * - Alle Swing-Änderungen auf dem EDT.
 * - Stellt (optional) die StatusBarEventQueue als Source ein.
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
        loadUsersFromRegistryOnInit();  // einmalig laden
        setSelectedUser("<Keinen>");
        setRightText("Bereit");

        // >>> Queue hier anbinden (statt in MainWindow):
        StatusBarEventQueue.getInstance().setSink(this::setMessage);
        StatusBarEventQueue.getInstance().setMinDisplayMillis(3000);
    }

    // ----- Public API -----

    /** Liefert die fertige Statusbar-Komponente zur Platzierung im SOUTH. */
    public JComponent getComponent() { return root; }

    /** Setze linken Status-Text (nur UI). */
    public void setMessage(final String msg) {
        runOnEdt(() -> leftLabel.setText(msg != null ? msg : ""));
    }

    /** Optional: Nachricht über die Queue anzeigen (mind. 3s sichtbar). */
    public void postStatus(String msg) {
        if (msg != null) {
            StatusBarEventQueue.getInstance().post(msg);
        }
    }

    /** Setze rechte AUX-Komponente (nur UI). */
    public void setRightComponent(final JComponent comp) {
        runOnEdt(() -> {
            auxRightBox.removeAll();
            if (comp != null) auxRightBox.add(comp);
            auxRightBox.revalidate();
            auxRightBox.repaint();
        });
    }

    /** Setze rechten AUX-Text (nur UI). */
    public void setRightText(final String text) {
        setRightComponent(new JLabel(text != null ? text : ""));
    }

    /**
     * Wähle den angezeigten Benutzer (Label + Combo).
     * Falls Name nicht im Modell vorhanden ist, wird er eingefügt
     * (außer null/blank -> "<Keinen>").
     */
    public void setSelectedUser(final String usernameOrNull) {
        final String name = normalizeName(usernameOrNull);
        runOnEdt(() -> {
            ensureNamePresentInModel(name);
            userCombo.setSelectedItem(name);
            leftLabel.setText("Aktiver Benutzer: " + name);
        });
    }

    /** Setze die Benutzerliste direkt (ereignisfrei). */
    public void setUsers(final List<String> usernames) {
        final List<String> safe = (usernames != null) ? new ArrayList<>(usernames) : new ArrayList<>();
        ensurePlaceholderFirst(safe);
        runOnEdt(() -> {
            String selected = currentSelectionOrNone();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(safe.toArray(new String[0]));
            userCombo.setModel(model);
            if (!contains(model, selected)) selected = "<Keinen>";
            userCombo.setSelectedItem(selected);
        });
    }

    /** Benutzerliste einmalig aus Registry (manuell; ereignisfrei). */
    public void refreshUsersFromRegistry() {
        setUsers(collectUsernamesFromRegistry());
    }

    // ----- Internal UI setup -----

    private void buildUi() {
        root.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 50)));
        rightWrap.setOpaque(false);
        auxRightBox.setOpaque(false);
        userBox.setOpaque(false);

        userCombo.setPrototypeDisplayValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ"); // stabile Breite
        userCombo.setFocusable(false);
        userCombo.setEnabled(true); // rein visuell

        rightWrap.add(auxRightBox);
        rightWrap.add(new JSeparator(SwingConstants.VERTICAL) {{ setPreferredSize(new Dimension(6, 18)); }});
        userBox.add(userPrefix);
        userBox.add(userCombo);
        rightWrap.add(userBox);

        root.add(leftLabel, BorderLayout.WEST);
        root.add(rightWrap, BorderLayout.EAST);
    }

    private void loadUsersFromRegistryOnInit() {
        List<String> names = collectUsernamesFromRegistry();
        ensurePlaceholderFirst(names);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(names.toArray(new String[0]));
        userCombo.setModel(model);
        userCombo.setSelectedItem("<Keinen>");
    }

    // ----- Helpers -----

    /** Sicherstellen, dass "name" im Modell existiert; ggf. einfügen. */
    private void ensureNamePresentInModel(String name) {
        ComboBoxModel<String> model = userCombo.getModel();
        if (model == null) {
            List<String> base = new ArrayList<>();
            base.add("<Keinen>");
            if (!"<Keinen>".equals(name)) base.add(name);
            userCombo.setModel(new DefaultComboBoxModel<>(base.toArray(new String[0])));
            return;
        }
        if (!contains(model, name)) {
            List<String> values = new ArrayList<>();
            boolean hasNone = false;
            for (int i = 0; i < model.getSize(); i++) {
                String v = model.getElementAt(i);
                if ("<Keinen>".equals(v)) hasNone = true;
                values.add(v);
            }
            if (!hasNone) values.add(0, "<Keinen>");
            if (!"<Keinen>".equals(name)) values.add(name);
            userCombo.setModel(new DefaultComboBoxModel<>(values.toArray(new String[0])));
        }
    }

    private static boolean contains(ComboBoxModel<String> model, String value) {
        if (model == null || value == null) return false;
        for (int i = 0; i < model.getSize(); i++) {
            if (value.equals(model.getElementAt(i))) return true;
        }
        return false;
    }

    private static void ensurePlaceholderFirst(List<String> names) {
        if (names == null) return;
        boolean hasNone = false;
        for (String n : names) { if ("<Keinen>".equals(n)) { hasNone = true; break; } }
        if (!hasNone) {
            names.add(0, "<Keinen>");
        } else if (!names.isEmpty() && !"<Keinen>".equals(names.get(0))) {
            List<String> copy = new ArrayList<>(names);
            names.clear();
            names.add("<Keinen>");
            for (String v : copy) if (!"<Keinen>".equals(v)) names.add(v);
        }
    }

    private static String normalizeName(String n) {
        if (n == null) return "<Keinen>";
        String t = n.trim();
        return t.isEmpty() ? "<Keinen>" : t;
    }

    private static List<String> collectUsernamesFromRegistry() {
        List<String> names = new ArrayList<>();
        names.add("<Keinen>");
        List<UserRegistry.User> all = UserRegistry.getInstance().getAll();
        if (all != null) {
            for (UserRegistry.User u : all) {
                if (u != null && u.getUsername() != null) names.add(u.getUsername());
            }
        }
        return names;
    }

    private String currentSelectionOrNone() {
        Object sel = userCombo.getSelectedItem();
        return sel == null ? "<Keinen>" : sel.toString();
    }

    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
