package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den globalen Root-Scope.
 *
 * Tabs:
 *  - BeforeAll
 *  - BeforeEach
 *  - Templates
 *
 * Rechts oben ein Speichern-Button (schreibt tests.json über TestRegistry.save()).
 * Jede Tabelle hat + und – zum Hinzufügen/Entfernen und kann inline editiert werden.
 *
 * Semantik:
 *  - BeforeAll (root.getBeforeAll()):
 *        Variablen, die EINMAL ganz am Anfang evaluiert werden.
 *        -> landen nicht im Dropdown der WHEN-Values
 *
 *  - BeforeEach (root.getBeforeEach()):
 *        Variablen, die vor jedem TestCase evaluiert werden.
 *        -> tauchen im Dropdown der WHEN-Values als normale Namen auf
 *
 *  - Templates (root.getTemplates()):
 *        Funktionszeiger (lazy ausgewertet in WHEN).
 *        -> tauchen im Dropdown mit führendem * auf
 */
public class RootScopeEditorTab extends JPanel {

    private final RootNode root;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public RootScopeEditorTab(RootNode root) {
        super(new BorderLayout());
        this.root = root;

        // Ensure default OTP template exists once at Root (lazy & user-basiert)
        try {
            if (root.getTemplates() != null && !root.getTemplates().containsKey("OTP")) {
                root.getTemplates().put("OTP", "{{otp({{user}})}}");
            }
        } catch (Throwable ignore) {
            // Keep UI resilient even if root/templates are null; MapTablePanel handles null backing.
        }

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Root Scope", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("💾 Speichern");
        saveBtn.setToolTipText("Tests speichern");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            JOptionPane.showMessageDialog(
                    RootScopeEditorTab.this,
                    "Gespeichert.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        savePanel.add(saveBtn);

        header.add(title, BorderLayout.CENTER);
        header.add(savePanel, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(header, BorderLayout.NORTH);

        // WICHTIG:
        // - BeforeAll: usersProvider != null  → MapTablePanel zeigt oben die "user"-Dropdown-Zeile (ausgegraut)
        // - BeforeEach / Templates: usersProvider == null → keine "user"-Zeile
        innerTabs.addTab("BeforeAll",
                new MapTablePanel(root.getBeforeAll(), "BeforeAll",
                        UserRegistry.getInstance().usernamesSupplier()));
        innerTabs.addTab("BeforeEach",
                new MapTablePanel(root.getBeforeEach(), "BeforeEach", null));
        innerTabs.addTab("Templates",
                new MapTablePanel(root.getTemplates(), "Templates", null));

        add(innerTabs, BorderLayout.CENTER);
    }

}

