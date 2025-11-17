package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.components.JTabbedPaneWithHelp;
import de.bund.zrb.ui.components.RoundIconButton;
import de.bund.zrb.ui.tabs.GivenListEditorTab;
import de.bund.zrb.ui.tabs.PreconditionListValidator;
import de.bund.zrb.ui.tabs.Saveable;
import de.bund.zrb.ui.tabs.Revertable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RootScopeEditorTab extends JPanel implements Saveable, Revertable {

    private RootNode root; // nicht final, damit Revert neu binden kann
    private JTabbedPaneWithHelp innerTabs;

    public RootScopeEditorTab(RootNode root) {
        super(new BorderLayout());
        this.root = root;
        buildUIFromRoot(this.root);
    }

    private void buildUIFromRoot(RootNode model) {
        removeAll();

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Root Scope", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        header.add(title, BorderLayout.CENTER);
        header.add(new JPanel(new FlowLayout(FlowLayout.RIGHT)), BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(header, BorderLayout.NORTH);

        innerTabs = new JTabbedPaneWithHelp();

        List<Precondtion> preconditions = model.getPreconditions();
        boolean needImmediateSave = false;
        if (preconditions == null) {
            preconditions = new ArrayList<Precondtion>();
            model.setPreconditions(preconditions);
            needImmediateSave = true;
        }

        // Reihenfolge: Templates, BeforeAll, BeforeEach, Preconditions, AfterEach
        innerTabs.addTab(
                "Templates",
                new MapTablePanel(
                        model.getTemplates(),
                        model.getTemplatesEnabled(),
                        "Templates",
                        null,
                        "OTP",
                        "{{otp({{user}})}}"
                )
        );

        innerTabs.addTab(
                "BeforeAll",
                new MapTablePanel(
                        model.getBeforeAll(), model.getBeforeAllEnabled(), model.getBeforeAllDesc(),
                        "BeforeAll",
                        de.bund.zrb.service.UserRegistry.getInstance().usernamesSupplier()
                )
        );

        innerTabs.addTab(
                "BeforeEach",
                new MapTablePanel(
                        model.getBeforeEach(), model.getBeforeEachEnabled(), model.getBeforeEachDesc(),
                        "BeforeEach",
                        null
                )
        );

        GivenListEditorTab preconditionsTab = new GivenListEditorTab("Root Scope", preconditions);
        innerTabs.addTab("Preconditions", preconditionsTab);
        boolean preconditionsValid = true;
        try {
            PreconditionListValidator.validateOrThrow("Root Scope", preconditions);
            preconditionsTab.clearValidationError();
        } catch (Exception ex) {
            preconditionsValid = false;
            preconditionsTab.showValidationError(ex.getMessage());
        }

        String pinnedKey = "screenshot";
        String pinnedValue = "{{screenshotfor({{user}})}}";
        innerTabs.addTab(
                "AfterEach",
                new AssertionTablePanel(
                        model.getAfterEach(), model.getAfterEachEnabled(), model.getAfterEachDesc(),
                        model.getAfterEachValidatorType(), model.getAfterEachValidatorValue(),
                        "AfterEach", pinnedKey, pinnedValue
                )
        );

        add(innerTabs, BorderLayout.CENTER);
        installHelpButton();

        if (needImmediateSave) {
            try { TestRegistry.getInstance().save(); } catch (Throwable ignore) { }
        }
        if (!preconditionsValid) {
            int preIdx = innerTabs.indexOfComponent(preconditionsTab);
            for (int i = 0; i < innerTabs.getTabCount(); i++) {
                if (i == preIdx) continue;
                innerTabs.setEnabledAt(i, false);
            }
            innerTabs.setSelectedIndex(preIdx);
        }

        revalidate();
        repaint();
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

    @Override
    public void saveChanges() {
        TestRegistry.getInstance().save();
    }

    @Override
    public void revertChanges() {
        // Neu laden und UI neu aufbauen
        TestRegistry.getInstance().load();
        this.root = TestRegistry.getInstance().getRoot();
        buildUIFromRoot(this.root);
    }
}
