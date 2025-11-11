package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.RootNode;
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
    private final JTabbedPaneWithHelp innerTabs = new JTabbedPaneWithHelp();

    public RootScopeEditorTab(RootNode root) {
        super(new BorderLayout());
        this.root = root;

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Root Scope", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("💾 Speichern");
        saveBtn.setToolTipText("Tests speichern");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            //DEBUG:
//            JOptionPane.showMessageDialog(
//                    RootScopeEditorTab.this,
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

        List<Precondtion> preconditions = root.getPreconditions();
        boolean needImmediateSave = false;
        if (preconditions == null) {
            preconditions = new ArrayList<Precondtion>();
            root.setPreconditions(preconditions);
            needImmediateSave = true;
        }
        GivenListEditorTab preconditionsTab = new GivenListEditorTab("Root Scope", preconditions);
        innerTabs.insertTab("Preconditions", null, preconditionsTab, "Globale Preconditions", 0);

        boolean preconditionsValid = true;
        try {
            PreconditionListValidator.validateOrThrow("Root Scope", preconditions);
            preconditionsTab.clearValidationError();
        } catch (Exception ex) {
            preconditionsValid = false;
            preconditionsTab.showValidationError(ex.getMessage());
        }

        if (needImmediateSave) {
            try { TestRegistry.getInstance().save(); } catch (Throwable ignore) { }
        }

        // BeforeAll: User-Dropdown aktiv (wie gehabt)
        innerTabs.addTab("BeforeAll",
                new MapTablePanel(root.getBeforeAll(), root.getBeforeAllEnabled(), "BeforeAll",
                        /* usersProvider */ de.bund.zrb.service.UserRegistry.getInstance().usernamesSupplier(),
                        /* pinnedKey */ null,
                        /* pinnedValue */ null));

        // BeforeEach: kein User-Dropdown, keine Pinned-Zeile
        innerTabs.addTab("BeforeEach",
                new MapTablePanel(root.getBeforeEach(), root.getBeforeEachEnabled(), "BeforeEach",
                        /* usersProvider */ null,
                        /* pinnedKey */ "home",
                        /* pinnedValue */ "{{navigateToStartPage({{user}})}}"));

        // Templates (ROOT): gepinnte OTP-Zeile
        innerTabs.addTab(
                "Templates",
                new MapTablePanel(
                        root.getTemplates(),
                        root.getTemplatesEnabled(),
                        "Templates",
                        null,                 // kein User-Dropdown
                        "OTP",                // gepinnter Key
                        "{{otp({{user}})}}"   // Default-Wert
                )
        );

        // AfterEach (Root) – gepinnt: Screenshot-Expression, Checkbox editierbar
        String pinnedKey   = "screenshot";
        String pinnedValue = "{{screenshotfor({{user}})}}"; // Built-in-Function
        innerTabs.addTab("AfterEach",
                new AssertionTablePanel(root.getAfterEach(), root.getAfterEachEnabled(), root.getAfterEachDesc(),
                        root.getAfterEachValidatorType(), root.getAfterEachValidatorValue(),
                        "AfterEach", pinnedKey, pinnedValue));

        add(innerTabs, BorderLayout.CENTER);
        installHelpButton();

        if (!preconditionsValid) {
            disableTabsFromIndex(1);
            innerTabs.setSelectedIndex(0);
        }
    }

    private void disableTabsFromIndex(int startIndex) {
        for (int i = startIndex; i < innerTabs.getTabCount(); i++) {
            innerTabs.setEnabledAt(i, false);
        }
    }

    private void installHelpButton() {
        RoundIconButton help = new RoundIconButton("?");
        help.setToolTipText("Hilfe zum Root-Scope anzeigen");
        help.addActionListener(e -> {
            String html = buildRootHelpHtml();
            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(wrapHtml(html)),
                    "Hilfe – Root-Scope",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        innerTabs.setHelpComponent(help);
    }

    private String buildRootHelpHtml() {
        StringBuilder sb = new StringBuilder(1200);
        sb.append("<html><body style='font-family:sans-serif;padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Root-Scope</h3>");
        sb.append("<ul>");
        sb.append("<li><b>Preconditions</b>: globale Vorbedingungen (vor WHEN), gelten für alle Suites/Cases.</li>");
        sb.append("<li><b>BeforeAll</b>: einmal zu Laufbeginn (globale Konstanten).</li>");
        sb.append("<li><b>BeforeEach</b>: vor jedem Case einmal (globale, frische Werte je Case).</li>");
        sb.append("<li><b>Templates</b>: lazy Expressions für globale Wiederverwendung (z. B. OTP).</li>");
        sb.append("<li><b>AfterEach</b>: Assertions nach jedem Case (Validator-Typ/Value).</li>");
        sb.append("</ul>");
        sb.append("<p>Shadow-Reihenfolge: <code>Case → Suite → Root</code>. Root ist die unterste Ebene.</p>");
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
