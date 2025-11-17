package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.components.JTabbedPaneWithHelp;
import de.bund.zrb.ui.components.RoundIconButton;
import de.bund.zrb.ui.tabs.GivenListEditorTab;
import de.bund.zrb.ui.tabs.Saveable;
import de.bund.zrb.ui.tabs.Revertable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SuiteScopeEditorTab extends JPanel implements Saveable, Revertable {

    private TestSuite suite; // nicht final, damit Revert neu binden kann
    private String suiteId;  // zur Re-Lookup nach Load
    private JTabbedPaneWithHelp innerTabs;
    private JTextField nameField;

    public SuiteScopeEditorTab(TestSuite suite) {
        super(new BorderLayout());
        this.suite = suite;
        this.suiteId = (suite != null ? suite.getId() : null);
        buildUIFromSuite(this.suite);
    }

    private void buildUIFromSuite(TestSuite model) {
        removeAll();

        JPanel header = new JPanel(new BorderLayout());
        JPanel headerInner = new JPanel(new BorderLayout());
        headerInner.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        JLabel headerLabel = new JLabel("Name:");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));
        nameField = new JTextField(safe(model != null ? model.getName() : ""));
        Font baseFont = nameField.getFont();
        if (baseFont != null) {
            nameField.setFont(baseFont.deriveFont(Font.BOLD, Math.min(22f, baseFont.getSize() + 8f)));
        }
        nameField.setBackground(new Color(250,250,235));
        nameField.setToolTipText("Name der Testsuite (wird im Baum angezeigt).");
        headerInner.add(headerLabel, BorderLayout.NORTH);
        headerInner.add(nameField, BorderLayout.CENTER);
        header.add(headerInner, BorderLayout.CENTER);
        header.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        innerTabs = new JTabbedPaneWithHelp();

        List<Precondtion> preconditions = (model != null) ? model.getPreconditions() : null;
        if (preconditions == null) {
            preconditions = new ArrayList<>();
            if (model != null) model.setPreconditions(preconditions);
        }
        String scopeLabel = "Suite " + safe(model != null ? model.getName() : "");
        GivenListEditorTab preconditionsTab = new GivenListEditorTab(scopeLabel, preconditions);

        // Reihenfolge: Templates, BeforeAll, BeforeEach, Preconditions, AfterAll
        innerTabs.addTab("Templates",
                new MapTablePanel(model.getTemplates(), model.getTemplatesEnabled(), "Templates", null));

        innerTabs.addTab("BeforeAll",
                new MapTablePanel(model.getBeforeAll(), model.getBeforeAllEnabled(), model.getBeforeAllDesc(), "BeforeAll",
                        UserRegistry.getInstance().usernamesSupplier()));

        innerTabs.addTab("BeforeEach",
                new MapTablePanel(model.getBeforeEach(), model.getBeforeEachEnabled(), model.getBeforeEachDesc(), "BeforeEach", null));

        innerTabs.addTab("Preconditions", preconditionsTab);

        innerTabs.addTab("AfterAll",
                new AssertionTablePanel(model.getAfterAll(), model.getAfterAllEnabled(), model.getAfterAllDesc(),
                        model.getAfterAllValidatorType(), model.getAfterAllValidatorValue(),
                        "AfterAll", null, null));

        add(innerTabs, BorderLayout.CENTER);
        installHelpButton();

        revalidate();
        repaint();
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }

    private void installHelpButton() {
        Boolean hide = SettingsService.getInstance().get("ui.helpButtons.hide", Boolean.class);
        if (Boolean.TRUE.equals(hide)) return;
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
        StringBuilder sb = new StringBuilder(1600);
        sb.append("<html><body style='font-family:sans-serif;padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Suite-Scope</h3>");
        sb.append("<ul>");
        sb.append("<li><b>Preconditions</b>: gelten für die gesamte Suite. ")
                .append("Sie laufen vor den WHEN-Schritten der Cases und sehen den bereits aufgebauten Case-Kontext. ")
                .append("Case-spezifische Preconditions können Werte überschreiben.</li>");
        sb.append("<li><b>BeforeAll</b>: einmal je Suite ausgewertet. ")
                .append("Typisch für Konstanten und Konfigurationswerte, die allen Cases der Suite zur Verfügung stehen.</li>");
        sb.append("<li><b>BeforeEach</b>: wird für jeden Case der Suite ausgeführt. ")
                .append("Erzeugt frische Werte je Case (z. B. Login-Token), falls die Variable noch nicht durch den Case gesetzt wurde.</li>");
        sb.append("<li><b>Templates</b>: lazy berechnete Werte, die erst bei Nutzung expandieren.</li>");
        sb.append("<li><b>AfterAll</b>: Assertions über die Suite hinweg (mit Validatoren), z. B. Aggregatszustände.</li>");
        sb.append("</ul>");

        sb.append("<p><b>Auswertungsreihenfolge pro Case innerhalb dieser Suite</b> ")
                .append("(nur wenn der Variablenname noch nicht belegt ist):</p>");
        sb.append("<ol>");
        sb.append("<li>Case: <code>Before</code></li>");
        sb.append("<li>Suite: <code>BeforeAll</code></li>");
        sb.append("<li>Root: <code>BeforeAll</code></li>");
        sb.append("<li>Suite: <code>BeforeEach</code></li>");
        sb.append("<li>Root: <code>BeforeEach</code></li>");
        sb.append("</ol>");

        sb.append("<p>Auch hier gilt: Jede Ebene ergänzt nur fehlende Variablen. ")
                .append("Case-Werte bleiben unangetastet, Suite-Werte überschreiben Root nur, wenn noch kein Wert vorhanden ist.</p>");
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

    @Override
    public void saveChanges() {
        if (suite != null) {
            String n = nameField.getText();
            suite.setName(n != null ? n.trim() : "");
        }
        TestRegistry.getInstance().save();
    }

    @Override
    public void revertChanges() {
        TestRegistry.getInstance().load();
        TestSuite reloaded = (suiteId != null) ? TestRegistry.getInstance().findSuiteById(suiteId) : null;
        if (reloaded == null) reloaded = this.suite; // Fallback
        this.suite = reloaded;
        buildUIFromSuite(this.suite);
    }
}
