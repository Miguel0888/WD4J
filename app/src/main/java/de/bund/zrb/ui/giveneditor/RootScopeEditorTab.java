package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.SettingsService;
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
                new MapTablePanel(root.getBeforeAll(), root.getBeforeAllEnabled(), root.getBeforeAllDesc(), "BeforeAll",
                        /* usersProvider */ de.bund.zrb.service.UserRegistry.getInstance().usernamesSupplier()));

        // BeforeEach: kein User-Dropdown, keine Pinned-Zeile
        innerTabs.addTab("BeforeEach",
                new MapTablePanel(root.getBeforeEach(), root.getBeforeEachEnabled(), root.getBeforeEachDesc(), "BeforeEach",
                        /* usersProvider */ null));

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
        Boolean hide = SettingsService.getInstance().get("ui.helpButtons.hide", Boolean.class);
        if (Boolean.TRUE.equals(hide)) return;
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
        StringBuilder sb = new StringBuilder(1600);
        sb.append("<html><body style='font-family:sans-serif;padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Root-Scope</h3>");
        sb.append("<ul>");
        sb.append("<li><b>Preconditions</b>: globale Vorbedingungen (vor WHEN), gelten für alle Suites/Cases.</li>");
        sb.append("<li><b>BeforeAll</b>: einmal zu Laufbeginn ausgewertet. ")
                .append("Typisch für globale Konstanten und Umgebungskonfiguration.</li>");
        sb.append("<li><b>BeforeEach</b>: vor jedem Case einmal. ")
                .append("Setzt globale, frische Werte je Case (z. B. Basis-URLs, Default-User), sofern noch kein Wert existiert.</li>");
        sb.append("<li><b>Templates</b>: lazy Expressions für globale Wiederverwendung (z. B. OTP).</li>");
        sb.append("<li><b>AfterEach</b>: Assertions nach jedem Case (Validator-Typ/Value), z. B. Screenshots oder Health-Checks.</li>");
        sb.append("</ul>");

        sb.append("<p>Pro Case wird ein leerer Variablenkontext aufgebaut und dann in dieser Reihenfolge gefüllt ")
                .append("(immer nur, wenn der Name noch nicht belegt ist):</p>");
        sb.append("<ol>");
        sb.append("<li>Case: <code>Before</code></li>");
        sb.append("<li>Suite: <code>BeforeAll</code></li>");
        sb.append("<li>Root: <code>BeforeAll</code></li>");
        sb.append("<li>Suite: <code>BeforeEach</code></li>");
        sb.append("<li>Root: <code>BeforeEach</code></li>");
        sb.append("</ol>");

        sb.append("<p>Root ist die unterste Ebene: Es liefert Standardwerte, die von Suite und Case bei Bedarf überschrieben werden.</p>");
        sb.append("<p>Shadow-Reihenfolge beim Zugriff: <code>Case → Suite → Root</code>. ")
                .append("Case-Werte haben die höchste Priorität.</p>");
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
