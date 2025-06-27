package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.Command;
import de.bund.zrb.ui.commandframework.CommandContext;
import de.bund.zrb.ui.commandframework.CommandRegistry;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Left drawer: tree view + green play button + drag & drop.
 */
public class LeftDrawer extends JPanel {

    private final CommandRegistry commandRegistry;
    private final JTree testTree;

    public LeftDrawer(CommandRegistry registry) {
        super(new BorderLayout());
        this.commandRegistry = registry;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Testsuites");
        root.add(new DefaultMutableTreeNode("Suite 1"));
        root.add(new DefaultMutableTreeNode("Suite 2"));
        testTree = new JTree(root);

        // 📌 Drag & Drop aktivieren:
        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());

        JScrollPane treeScroll = new JScrollPane(testTree);

        JButton playButton = new JButton("▶");
        playButton.setBackground(Color.GREEN);
        playButton.setFocusPainted(false);

        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = testTree.getLastSelectedPathComponent();
                if (selected != null) {
                    Command playCommand = commandRegistry.getCommand("testsuite.play");
                    CommandContext ctx = new CommandContext();
                    ctx.put("suite", selected.toString());
                    playCommand.execute(ctx);
                }
            }
        });

        setupContextMenu();

        add(playButton, BorderLayout.NORTH);
        add(treeScroll, BorderLayout.CENTER);
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem newSuite = new JMenuItem("Neue Testsuite");
        JMenuItem rename = new JMenuItem("Umbenennen");
        JMenuItem delete = new JMenuItem("Löschen");
        JMenuItem properties = new JMenuItem("Eigenschaften");

        newSuite.addActionListener(e -> createNewSuite());
        rename.addActionListener(e -> renameNode());
        delete.addActionListener(e -> deleteNode());
        properties.addActionListener(e -> openPropertiesDialog());

        contextMenu.add(newSuite);
        contextMenu.add(rename);
        contextMenu.add(delete);
        contextMenu.addSeparator();
        contextMenu.add(properties);

        testTree.setComponentPopupMenu(contextMenu);
    }

    private void createNewSuite() {
        String name = JOptionPane.showInputDialog(this, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            DefaultMutableTreeNode selected = getSelectedNodeOrRoot();
            DefaultMutableTreeNode newSuite = new DefaultMutableTreeNode(name);
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.insertNodeInto(newSuite, selected, selected.getChildCount());
        }
    }

    private void renameNode() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) { // Root nicht umbenennen
            String name = JOptionPane.showInputDialog(this, "Neuer Name:", selected.toString());
            if (name != null && !name.trim().isEmpty()) {
                selected.setUserObject(name);
                ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
            }
        }
    }

    private void deleteNode() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) { // Root nicht löschen
            ((DefaultTreeModel) testTree.getModel()).removeNodeFromParent(selected);
        }
    }

    private void openPropertiesDialog() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            PropertiesDialog dialog = new PropertiesDialog(selected.toString());
            dialog.setVisible(true);
        }
    }

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) testTree.getLastSelectedPathComponent();
    }

    private DefaultMutableTreeNode getSelectedNodeOrRoot() {
        DefaultMutableTreeNode selected = getSelectedNode();
        return selected != null ? selected : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }


}
