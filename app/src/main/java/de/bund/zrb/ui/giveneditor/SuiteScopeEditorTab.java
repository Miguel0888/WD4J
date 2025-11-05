package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den Suite-Scope.
 *
 * Tabs:
 *  - BeforeAll
 *  - BeforeEach
 *  - Templates
 *
 * Oben rechts: Speichern-Button, der aktuell einfach TestRegistry.save() aufruft.
 * Die Tabellen haben + und – zum Hinzufügen/Entfernen.
 */
public class SuiteScopeEditorTab extends JPanel {

    private final TestSuite suite;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public SuiteScopeEditorTab(TestSuite suite) {
        super(new BorderLayout());
        this.suite = suite;

        JPanel header = new JPanel(new BorderLayout());

        JPanel textBlock = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Suite-Scope: " + safe(suite.getName()), SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea desc = new JTextArea();
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setText(safe(suite.getDescription()));

        textBlock.add(title, BorderLayout.NORTH);
        textBlock.add(desc, BorderLayout.CENTER);
        textBlock.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("💾 Speichern");
        saveBtn.setToolTipText("Änderungen dieser Suite in tests.json schreiben");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            //DEBUG:
//            JOptionPane.showMessageDialog(
//                    SuiteScopeEditorTab.this,
//                    "Gespeichert.",
//                    "Info",
//                    JOptionPane.INFORMATION_MESSAGE
//            );
        });
        savePanel.add(saveBtn);
        savePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        header.add(textBlock, BorderLayout.CENTER);
        header.add(savePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        innerTabs.addTab("BeforeAll",   new MapTablePanel(suite.getBeforeAll(),   "BeforeAll", UserRegistry.getInstance().usernamesSupplier()));
        innerTabs.addTab("BeforeEach",  new MapTablePanel(suite.getBeforeEach(),  "BeforeEach", null));
        innerTabs.addTab("Templates",   new MapTablePanel(suite.getTemplates(),   "Templates", null));

        add(innerTabs, BorderLayout.CENTER);
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }
}

