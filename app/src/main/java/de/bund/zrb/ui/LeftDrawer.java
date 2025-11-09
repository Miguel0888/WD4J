package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.tabs.TabManager;
import de.bund.zrb.ui.leftdrawer.PrecondTreeController;
import de.bund.zrb.ui.leftdrawer.TestTreeCellRenderer;
import de.bund.zrb.ui.leftdrawer.TestTreeController;

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
    private final JTabbedPane leftTabs;

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
        leftTabs = new JTabbedPane();
        leftTabs.addTab("Tests", testScroll);
        leftTabs.addTab("Preconditions", precondScroll);
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
}
