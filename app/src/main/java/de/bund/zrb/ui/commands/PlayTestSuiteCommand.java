package de.bund.zrb.ui.commands;

import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.tabs.RunnerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Open the Runner tab and start playback. Provide a closable tab header with a red "×" button.
 */
public class PlayTestSuiteCommand extends ShortcutMenuCommand {

    private final JTabbedPane tabbedPane;

    public PlayTestSuiteCommand(JTabbedPane tabbedPane) {
        super();
        this.tabbedPane = tabbedPane;
    }

    @Override
    public String getId() {
        return "testsuite.play";
    }

    @Override
    public String getLabel() {
        return "Testsuite ausführen";
    }

    @Override
    public void perform() {
        System.out.println("▶ Starte Playback...");

        // Tab öffnen
        final RunnerPanel runnerPanel = new RunnerPanel();
        TestPlayerService.getInstance().registerLogger(runnerPanel.getLogger());

        // Add tab and select it
        tabbedPane.addTab("Runner", runnerPanel);
        final int tabIndex = tabbedPane.indexOfComponent(runnerPanel);
        tabbedPane.setTabComponentAt(tabIndex, createClosableTabHeader("Runner", tabbedPane, runnerPanel));
        tabbedPane.setSelectedComponent(runnerPanel);

        // Test starten (Hintergrund-Thread)
        new Thread(new Runnable() {
            public void run() {
                TestPlayerService.getInstance().runSuites();
            }
        }, "WD4J-Runner").start();
    }

    /**
     * Create a closable tab header: "Runner   [×]".
     * The close button stops playback and removes the tab.
     */
    private JComponent createClosableTabHeader(final String title,
                                               final JTabbedPane tabs,
                                               final Component tabContent) {
        final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);

        final JLabel label = new JLabel(title);
        header.add(label);

        final JButton close = new JButton("\u00D7"); // '×'
        close.setMargin(new Insets(0, 6, 0, 6));
        close.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        close.setOpaque(false);
        close.setContentAreaFilled(false);
        close.setFocusable(false);
        close.setToolTipText("Tab schließen");
        close.setForeground(new Color(200, 0, 0)); // show as red

        // Make button a bit bolder and larger without relying on platform fonts
        Font f = close.getFont();
        close.setFont(f.deriveFont(Font.BOLD, f.getSize2D() + 1f));

        close.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // Stop runner politely
                try {
                    TestPlayerService.getInstance().stopPlayback();
                } catch (Throwable ignore) {
                    // Ignore errors when stopping
                }

                // Remove the tab
                int idx = tabs.indexOfComponent(tabContent);
                if (idx >= 0) {
                    tabs.removeTabAt(idx);
                }
            }
        });

        header.add(close);
        return header;
    }
}
