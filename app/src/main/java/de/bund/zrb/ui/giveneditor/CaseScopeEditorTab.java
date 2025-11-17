package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.components.JTabbedPaneWithHelp;
import de.bund.zrb.ui.components.RoundIconButton;
import de.bund.zrb.ui.tabs.GivenListEditorTab;
import de.bund.zrb.ui.tabs.Saveable;
import de.bund.zrb.ui.tabs.Revertable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CaseScopeEditorTab extends JPanel implements Saveable, Revertable {

    private TestCase testCase; // nicht final für Rebind
    private String caseId;     // Lookup bei Revert
    private final JTabbedPaneWithHelp innerTabs = new JTabbedPaneWithHelp();
    private JTextField descField;

    public CaseScopeEditorTab(TestCase testCase) {
        super(new BorderLayout());
        this.testCase = testCase;
        this.caseId = (testCase != null ? testCase.getId() : null);
        buildUIFromCase(this.testCase);
    }

    private void buildUIFromCase(TestCase model) {
        removeAll();

        JPanel header = new JPanel(new BorderLayout());
        JPanel headerInner = new JPanel(new BorderLayout());
        headerInner.setBorder(BorderFactory.createEmptyBorder(12,12,6,12));
        JLabel headerLabel = new JLabel("Name:");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));
        descField = new JTextField(safe(model != null ? model.getName() : ""));
        Font bf = descField.getFont();
        if (bf != null) descField.setFont(bf.deriveFont(Font.BOLD, Math.min(22f, bf.getSize() + 8f)));
        descField.setBackground(new Color(250,250,235));
        descField.setToolTipText("Name des TestCase (wird im Baum angezeigt).");
        headerInner.add(headerLabel, BorderLayout.NORTH);
        headerInner.add(descField, BorderLayout.CENTER);
        header.add(headerInner, BorderLayout.CENTER);
        header.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        List<Precondtion> preconditions = (model != null) ? model.getPreconditions() : null;
        if (preconditions == null) {
            preconditions = new ArrayList<>();
            if (model != null) model.setPreconditions(preconditions);
        }
        String scopeLabel = "Case " + safe(model != null ? model.getName() : "");
        GivenListEditorTab preconditionsTab = new GivenListEditorTab(scopeLabel, preconditions);

        innerTabs.removeAllTabs();
        innerTabs.addTab("Templates",
                new MapTablePanel(model.getTemplates(), model.getTemplatesEnabled(), "Templates", null));
        innerTabs.addTab("Before",
                new MapTablePanel(model.getBefore(), model.getBeforeEnabled(), model.getBeforeDesc(), "Before",
                        UserRegistry.getInstance().usernamesSupplier()));
        innerTabs.addTab("Preconditions", preconditionsTab);
        innerTabs.addTab("After",
                new AssertionTablePanel(model.getAfter(), model.getAfterEnabled(), model.getAfterDesc(),
                        model.getAfterValidatorType(), model.getAfterValidatorValue(),
                        "After", null, null));

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
        if (testCase != null) {
            String d = descField.getText();
            testCase.setName(d != null ? d.trim() : "");
        }
        TestRegistry.getInstance().save();
    }

    @Override
    public void revertChanges() {
        TestRegistry.getInstance().load();
        TestCase reloaded = (caseId != null) ? TestRegistry.getInstance().findCaseById(caseId) : null;
        if (reloaded == null) reloaded = this.testCase; // Fallback
        this.testCase = reloaded;
        buildUIFromCase(this.testCase);
    }
}
