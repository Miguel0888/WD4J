package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.Command;
import de.bund.zrb.ui.commandframework.CommandContext;
import de.bund.zrb.ui.commandframework.CommandRegistry;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Left drawer: tree view + green play button.
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

        add(playButton, BorderLayout.NORTH);
        add(treeScroll, BorderLayout.CENTER);
    }
}
