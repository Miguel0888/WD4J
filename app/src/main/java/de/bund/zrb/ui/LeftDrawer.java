package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.leftdrawer.EditorTabOpener;
import de.bund.zrb.ui.leftdrawer.PrecondTreeController;
import de.bund.zrb.ui.leftdrawer.TestTreeCellRenderer;
import de.bund.zrb.ui.leftdrawer.TestTreeController;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    public LeftDrawer() {
        super(new BorderLayout());

        // --- Build test tree and populate ---
        testTree = TestTreeController.buildTestTree();
        testCtrl = new TestTreeController(testTree);
        testCtrl.refreshTestSuites(null);

        // Enable DnD and custom renderer for tests (keep existing behavior)
        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());
        testTree.setCellRenderer(new TestTreeCellRenderer());

        // Open editors on double click (tests)
        testTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = testTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        TestNode node = (TestNode) path.getLastPathComponent();
                        EditorTabOpener.openEditorTab(LeftDrawer.this, node);
                    }
                }
            }
        });

        // --- Build precondition tree and populate ---
        precondTree = PrecondTreeController.buildPrecondTree();
        precondCtrl = new PrecondTreeController(precondTree);
        precondCtrl.refreshPreconditions();

        // Use same renderer for a consistent look
        precondTree.setCellRenderer(new TestTreeCellRenderer());

        // Double click in preconditions: open ActionEditorTab for steps; ignore on precondition node for now
        precondTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = precondTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        TestNode node = (TestNode) path.getLastPathComponent();
                        Object ref = node.getModelRef();
                        if (ref instanceof TestAction) {
                            EditorTabOpener.openEditorTab(LeftDrawer.this, node);
                        }
                    }
                }
            }
        });

        // --- Wrap trees in scroll panes and add to a JTabbedPane ---
        JScrollPane testScroll = new JScrollPane(testTree);
        JScrollPane precondScroll = new JScrollPane(precondTree);

        leftTabs = new JTabbedPane();
        leftTabs.addTab("Tests", testScroll);
        leftTabs.addTab("Preconditions", precondScroll);

        // --- Play button (north) ---
//        JButton playButton = new JButton("▶");
//        playButton.setBackground(Color.GREEN);
//        playButton.setFocusPainted(false);
//        playButton.addActionListener(e -> {
//            MenuCommand playCommand = commandRegistry.getById("testsuite.play").get();
//            playCommand.perform();
//        });
//        add(playButton, BorderLayout.NORTH);

        // Context menu only for test tree (unchanged for now)
        testCtrl.setupContextMenu();

        add(leftTabs, BorderLayout.CENTER);

        // Refresh tests on save event (existing behavior)
        ApplicationEventBus.getInstance().subscribe(event -> {
            if (event instanceof TestSuiteSavedEvent) {
                testCtrl.refreshTestSuites((String) event.getPayload());
            }
            // Note: if you later emit a PreconditionSavedEvent, hook precondCtrl.refreshPreconditions() here similarly.
        });

        // Refresh on new Precond Events
        ApplicationEventBus.getInstance().subscribe(event -> {
            if (event instanceof TestSuiteSavedEvent) {
                testCtrl.refreshTestSuites((String) event.getPayload());
            }
            if (event instanceof de.bund.zrb.event.PreconditionSavedEvent) {
                precondCtrl.refreshPreconditions();
            }
        });

        TestPlayerService.getInstance().registerDrawer(this);
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
