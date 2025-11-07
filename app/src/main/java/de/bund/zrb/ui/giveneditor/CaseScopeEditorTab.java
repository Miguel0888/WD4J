package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den Case-Scope.
 *
 * Tabs:
 *  - Before     (Variablen, die vor diesem Case evaluiert werden)
 *  - Templates  (Funktionszeiger/lazy für diesen Case)
 *
 * Speichern-Button oben rechts + in jeder Tabellen-Toolbar.
 */
public class CaseScopeEditorTab extends JPanel {

    private final TestCase testCase;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public CaseScopeEditorTab(TestCase testCase) {
        super(new BorderLayout());
        this.testCase = testCase;

        // Header oben (Titel + Speichern)
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Case-Scope: " + safe(testCase.getName()), SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("💾 Speichern");
        saveBtn.setToolTipText("Änderungen dieses Case in tests.json schreiben");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            //DEBUG:
//            JOptionPane.showMessageDialog(
//                    CaseScopeEditorTab.this,
//                    "Gespeichert.",
//                    "Info",
//                    JOptionPane.INFORMATION_MESSAGE
//            );
        });
        savePanel.add(saveBtn);

        header.add(title, BorderLayout.CENTER);
        header.add(savePanel, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(header, BorderLayout.NORTH);

        // Tabs:
        // "Before"  == testCase.getBefore()
        // "Templates" == testCase.getTemplates()
        innerTabs.addTab("Before",    new MapTablePanel(testCase.getBefore(),    testCase.getBeforeEnabled(),    "Before",    UserRegistry.getInstance().usernamesSupplier()));
        innerTabs.addTab("Templates", new MapTablePanel(testCase.getTemplates(), testCase.getTemplatesEnabled(), "Templates", null));

        // After (Case) – frei editierbar, kein Pin
        innerTabs.addTab("After",
                new AssertionTablePanel(testCase.getAfter(), testCase.getAfterEnabled(), testCase.getAfterDesc(), "After", null, null));

        add(innerTabs, BorderLayout.CENTER);
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }
}
