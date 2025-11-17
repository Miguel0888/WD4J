package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.tabs.TabManager;
import de.bund.zrb.ui.leftdrawer.PrecondTreeController;
import de.bund.zrb.ui.leftdrawer.TestTreeCellRenderer;
import de.bund.zrb.ui.leftdrawer.TestTreeController;
import de.bund.zrb.ui.components.JTabbedPaneWithHelp;
import de.bund.zrb.ui.components.RoundIconButton;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.List;

/**
 * Left drawer: tabbed tree view (Tests & Preconditions) + green play button + drag & drop (tests).
 */
public class LeftDrawer extends JPanel implements TestPlayerUi {

    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();

    private final JTree testTree;
    private final JTree precondTree;
    private final JTabbedPaneWithHelp leftTabs;

    // Controllers (extracted)
    private final TestTreeController testCtrl;
    private final PrecondTreeController precondCtrl;

    // Referenz auf das zentrale Editor-Tab-Pane (aus MainWindow)
    private final JTabbedPane mainEditorTabs;
    private final TabManager tabManager; // neuer Service für Preview + persistente Tabs

    public LeftDrawer(JTabbedPane mainEditorTabs) {
        super(new BorderLayout());
        this.mainEditorTabs = mainEditorTabs; // merken für frühere Logik
        this.tabManager = new TabManager(this, mainEditorTabs);

        // --- Build test tree and populate ---
        testTree = TestTreeController.buildTestTree();
        testCtrl = new TestTreeController(testTree);
        testCtrl.setOpenHandler(tabManager);
        testCtrl.refreshTestTree();

        // Auswahl- und Tastatur-Preview (Tests)
        installPreviewBehavior(testTree);

        // Entfernte alte Doppelklick-Logik: kein automatisches persistentes Öffnen mehr
        // testTree.addMouseListener(...) entfernt

        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());
        testTree.setCellRenderer(new TestTreeCellRenderer());

        // --- Build precondition tree and populate ---
        precondTree = PrecondTreeController.buildPrecondTree();
        precondCtrl = new PrecondTreeController(precondTree);
        precondCtrl.setOpenHandler(tabManager);
        precondCtrl.refreshPreconditions();

        // Preview-Verhalten auch für Precond-Tree
        installPreviewBehavior(precondTree);

        precondTree.setCellRenderer(new TestTreeCellRenderer());
        precondCtrl.setupContextMenu();

        // Kontextmenüs für Tests erweitern ("In neuem Tab öffnen")
        testCtrl.setupContextMenu();

        // Wrap in Tabs
        JScrollPane testScroll = new JScrollPane(testTree);
        JScrollPane precondScroll = new JScrollPane(precondTree);
        leftTabs = new JTabbedPaneWithHelp();
        leftTabs.addTab("Tests", testScroll);
        leftTabs.addTab("Preconditions", precondScroll);
        installHelpButton();
        add(leftTabs, BorderLayout.CENTER);

        // Events für Refresh
        ApplicationEventBus.getInstance().subscribe(event -> {
            if (event instanceof TestSuiteSavedEvent) {
                testCtrl.refreshTestTree();
            }
        });
        ApplicationEventBus.getInstance().subscribe(event -> {
            if (event instanceof TestSuiteSavedEvent) {
                testCtrl.refreshTestTree();
            }
            if (event instanceof de.bund.zrb.event.PreconditionSavedEvent) {
                precondCtrl.refreshPreconditions();
            }
        });

        TestPlayerService.getInstance().registerDrawer(this);
    }

    // Fügt Listener für Auswahl (Single Click / Keyboard) hinzu und leitet in Preview
    private void installPreviewBehavior(JTree tree) {
        tree.getSelectionModel().addTreeSelectionListener(e -> {
            Object last = tree.getLastSelectedPathComponent();
            if (last instanceof TestNode) {
                tabManager.showInPreview((TestNode) last);
            }
        });
        tree.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER || e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    Object last = tree.getLastSelectedPathComponent();
                    if (last instanceof TestNode) {
                        tabManager.showInPreview((TestNode) last);
                    }
                }
            }
        });
    }

    // ========================= TestPlayerUi impl (delegate to controller) =========================

    @Override
    public TestNode getSelectedNode() {
        return testCtrl.getSelectedNode();
    }

    @Override
    public void updateNodeStatus(TestNode node, boolean passed) {
        testCtrl.updateNodeStatus(node, passed);
    }

    @Override
    public void updateSuiteStatus(TestNode suite) {
        testCtrl.updateSuiteStatus(suite);
    }

    @Override
    public TestNode getRootNode() {
        return testCtrl.getRootNode();
    }

    private DefaultMutableTreeNode getSelectedNodeOrRoot() {
        return testCtrl.getSelectedNodeOrRoot();
    }

    @Override
    public List<TestSuite> getSelectedSuites() {
        return testCtrl.getSelectedSuites();
    }

    private void installHelpButton() {
        Boolean hide = SettingsService.getInstance().get("ui.helpButtons.hide", Boolean.class);
        if (Boolean.TRUE.equals(hide)) return; // Help-Buttons global ausgeblendet

        RoundIconButton help = new RoundIconButton("?");
        help.setToolTipText("Hilfe zum Test-/Precondition-Baum anzeigen");
        help.addActionListener(e -> showLeftDrawerHelp());
        leftTabs.setHelpComponent(help);
    }

    private void showLeftDrawerHelp() {
        String html = buildLeftDrawerHelpHtml();
        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(wrapHtml(html)),
                "Hilfe – Tests & Preconditions",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String buildLeftDrawerHelpHtml() {
        StringBuilder sb = new StringBuilder(900);
        sb.append("<html><body style='font-family:sans-serif;padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Tests & Preconditions – Überblick</h3>");
        sb.append("<ul>");
        sb.append("<li><b>Tests</b>-Tab zeigt die Test-Suite-Hierarchie: Root → Suites → Cases → Actions.</li>");
        sb.append("<li><b>Preconditions</b>-Tab listet wiederverwendbare Vorbedingungen (Given-Blöcke / vorbereitende Aktionen).</li>");
        sb.append("<li><b>Preview</b>: Einfachklick oder ENTER/SPACE auf einen Knoten öffnet dessen Inhalt im Preview-Tab (kein persistentes Tab).</li>");
        sb.append("<li><b>Persistente Tabs</b>: Kontextmenü → 'In neuem Tab öffnen' oder wenn im Preview-Inhalt Änderungen vorgenommen werden.</li>");
        sb.append("<li><b>Pinning</b>: Ein bereits geöffneter persistenter Tab wird beim Klick auf denselben Knoten fokussiert statt Preview zu überschreiben.</li>");
        sb.append("<li><b>Drag & Drop</b>: Tests können per Drag & Drop innerhalb des Baums umsortiert werden (Suite-/Case-Ebene).</li>");
        sb.append("<li><b>Kontext-Menü</b>: Rechtsklick auf Nodes für Aktionen wie 'In neuem Tab öffnen'.</li>");
        sb.append("<li><b>Umbenennen (Actions)</b>: Doppelklick oder F2 zum In-Place-Umbenennen von Schritten. ENTER speichert, ESC bricht ab. Die Eingabe wird als Beschreibung (description) gespeichert und auch als Knotenbezeichnung verwendet.</li>");
        sb.append("<li><b>Preconditions</b>: Referenzen im Case/Suite/Root werden vor den WHEN-Schritten ausgeführt und erben den Variablen-Scope.</li>");
        sb.append("<li><b>Status</b>: Erfolgs-/Fehlerstatus eines Case/Suite wird nach Ausführung im Baum aktualisiert.</li>");
        sb.append("<li><b>Tastatur</b>: ↑/↓ Navigation, ENTER/SPACE für Preview, Kontextmenü via Shift+F10 (je nach OS).</li>");
        sb.append("</ul>");
        sb.append("<p style='color:#555'>Hinweis: Der Preview-Tab bleibt flüchtig. Änderungen darin fördern automatisch zu einem persistenten Tab.</p>");
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
