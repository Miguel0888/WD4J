package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.tabs.GivenListEditorTab;
import de.bund.zrb.ui.tabs.PreconditionListValidator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
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

        List<Precondtion> preconditions = testCase.getPreconditions();
        boolean needImmediateSave = false;
        if (preconditions == null) {
            preconditions = new ArrayList<Precondtion>();
            testCase.setPreconditions(preconditions);
            needImmediateSave = true;
        }
        String scopeLabel = "Case " + safe(testCase.getName());
        GivenListEditorTab preconditionsTab = new GivenListEditorTab(scopeLabel, preconditions);
        innerTabs.insertTab("Preconditions", null, preconditionsTab, "Case Preconditions", 0);

        boolean preconditionsValid = true;
        try {
            PreconditionListValidator.validateOrThrow(scopeLabel, preconditions);
            preconditionsTab.clearValidationError();
        } catch (Exception ex) {
            preconditionsValid = false;
            preconditionsTab.showValidationError(ex.getMessage());
        }

        if (needImmediateSave) {
            try { TestRegistry.getInstance().save(); } catch (Throwable ignore) { }
        }

        // Tabs:
        // "Before"  == testCase.getBefore()
        // "Templates" == testCase.getTemplates()
        innerTabs.addTab("Before",
                new MapTablePanel(testCase.getBefore(), testCase.getBeforeEnabled(), "Before",
                        UserRegistry.getInstance().usernamesSupplier()));
        innerTabs.addTab("Templates",
                new MapTablePanel(testCase.getTemplates(), testCase.getTemplatesEnabled(), "Templates", null));

        // After (Case) – frei editierbar, kein Pin
        innerTabs.addTab("After",
                new AssertionTablePanel(testCase.getAfter(), testCase.getAfterEnabled(), testCase.getAfterDesc(), "After", null, null));

        add(innerTabs, BorderLayout.CENTER);

        if (!preconditionsValid) {
            disableTabsFromIndex(1);
            innerTabs.setSelectedIndex(0);
        }
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }

    private void disableTabsFromIndex(int startIndex) {
        for (int i = startIndex; i < innerTabs.getTabCount(); i++) {
            innerTabs.setEnabledAt(i, false);
        }
    }
}
