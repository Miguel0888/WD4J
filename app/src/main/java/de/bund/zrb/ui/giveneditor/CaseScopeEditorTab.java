package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestCase;
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
    private final JTabbedPaneWithHelp innerTabs = new JTabbedPaneWithHelp();

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
                new AssertionTablePanel(testCase.getAfter(), testCase.getAfterEnabled(), testCase.getAfterDesc(),
                        testCase.getAfterValidatorType(), testCase.getAfterValidatorValue(),
                        "After", null, null));

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
        help.setToolTipText("Hilfe zum Case-Scope anzeigen");
        help.addActionListener(e -> {
            String html = buildCaseHelpHtml();
            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(wrapHtml(html)),
                    "Hilfe – Case-Scope",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        innerTabs.setHelpComponent(help);
    }

    private String buildCaseHelpHtml() {
        StringBuilder sb = new StringBuilder(1600);
        sb.append("<html><body style='font-family:sans-serif;padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Case-Scope</h3>");
        sb.append("<ul>");
        sb.append("<li><b>Preconditions</b>: werden vor den WHEN-Schritten des Cases ausgeführt. ")
                .append("Sie sehen bereits den kompletten Variablenkontext dieses Cases und können weitere Case-Variablen setzen.</li>");
        sb.append("<li><b>Before</b>: Variablen für diesen Case. ")
                .append("Sie werden als erstes für den Case ausgewertet und liefern die initialen Werte.</li>");
        sb.append("<li><b>Templates</b>: lazy Expressions, die erst bei Nutzung expandieren (z. B. OTP, Zeitstempel).</li>");
        sb.append("<li><b>After</b>: Assertions/Checks nach dem Case (mit Validator-Typ/Value).</li>");
        sb.append("</ul>");

        sb.append("<p><b>Auswertungsreihenfolge pro Case</b> (nur wenn der Variablenname noch nicht belegt ist):</p>");
        sb.append("<ol>");
        sb.append("<li>Case: <code>Before</code></li>");
        sb.append("<li>Suite: <code>BeforeAll</code></li>");
        sb.append("<li>Root: <code>BeforeAll</code></li>");
        sb.append("<li>Suite: <code>BeforeEach</code></li>");
        sb.append("<li>Root: <code>BeforeEach</code></li>");
        sb.append("</ol>");

        sb.append("<p>Jede Ebene ergänzt nur fehlende Variablen. Bereits gesetzte Werte werden nicht überschrieben.</p>");
        sb.append("<p>Beim Zugriff gilt weiterhin die Shadow-Reihenfolge: ")
                .append("<code>Case → Suite → Root</code>. ")
                .append("Case-Werte haben die höchste Priorität, Suite-Werte überschreiben Root, sofern ein Name noch frei ist.</p>");
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
