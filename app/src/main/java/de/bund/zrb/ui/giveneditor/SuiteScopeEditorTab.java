package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.components.JTabbedPaneWithHelp;
import de.bund.zrb.ui.components.RoundIconButton;
import de.bund.zrb.ui.tabs.GivenListEditorTab;
import de.bund.zrb.ui.tabs.PreconditionListValidator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
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
    private final JTabbedPaneWithHelp innerTabs = new JTabbedPaneWithHelp();

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

        List<Precondtion> preconditions = suite.getPreconditions();
        boolean needImmediateSave = false;
        if (preconditions == null) {
            preconditions = new ArrayList<Precondtion>();
            suite.setPreconditions(preconditions);
            needImmediateSave = true;
        }
        String scopeLabel = "Suite " + safe(suite.getName());
        GivenListEditorTab preconditionsTab = new GivenListEditorTab(scopeLabel, preconditions);
        innerTabs.insertTab("Preconditions", null, preconditionsTab, "Suite Preconditions", 0);

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

        innerTabs.addTab("BeforeAll",
                new MapTablePanel(suite.getBeforeAll(), suite.getBeforeAllEnabled(), "BeforeAll",
                        UserRegistry.getInstance().usernamesSupplier()));
        innerTabs.addTab("BeforeEach",
                new MapTablePanel(suite.getBeforeEach(), suite.getBeforeEachEnabled(), "BeforeEach", null));
        innerTabs.addTab("Templates",
                new MapTablePanel(suite.getTemplates(), suite.getTemplatesEnabled(), "Templates", null));

        innerTabs.addTab("AfterAll",
                new AssertionTablePanel(suite.getAfterAll(), suite.getAfterAllEnabled(), suite.getAfterAllDesc(),
                        suite.getAfterAllValidatorType(), suite.getAfterAllValidatorValue(),
                        "AfterAll", null, null));

        add(innerTabs, BorderLayout.CENTER);
        installHelpButton();

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

    private void installHelpButton() {
        RoundIconButton help = new RoundIconButton("?");
        help.setToolTipText("Hilfe zum Suite-Scope anzeigen");
        help.addActionListener(e -> {
            String html = buildSuiteHelpHtml();
            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(wrapHtml(html)),
                    "Hilfe – Suite-Scope",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        innerTabs.setHelpComponent(help);
    }

    private String buildSuiteHelpHtml() {
        StringBuilder sb = new StringBuilder(1200);
        sb.append("<html><body style='font-family:sans-serif;padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Suite-Scope</h3>");
        sb.append("<ul>");
        sb.append("<li><b>Preconditions</b>: gelten für die Suite (vor WHEN), erben Root-Variablen; Case kann überschreiben.</li>");
        sb.append("<li><b>BeforeAll</b>: einmal je Suite (Konstanten / Konfigurationswerte).</li>");
        sb.append("<li><b>BeforeEach</b>: pro Case der Suite (frische Werte je Case, z. B. Login, Token).</li>");
        sb.append("<li><b>Templates</b>: lazy berechnet bei Nutzung.</li>");
        sb.append("<li><b>AfterAll</b>: Assertions über die Suite hinweg (mit Validatoren).</li>");
        sb.append("</ul>");
        sb.append("<p>Shadow-Reihenfolge beim Zugriff: <code>Case → Suite → Root</code>.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private JEditorPane wrapHtml(String html) {
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        pane.setCaretPosition(0);
        return pane;
    }
}
