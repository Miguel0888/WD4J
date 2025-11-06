package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.Severity;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.tabs.RunnerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/** Open Runner tab and start playback; show status via EventBus. */
public class PlayTestSuiteCommand extends ShortcutMenuCommand {

    private final JTabbedPane tabbedPane;

    public PlayTestSuiteCommand(JTabbedPane tabbedPane) {
        super();
        this.tabbedPane = tabbedPane;
    }

    @Override
    public String getId() { return "testsuite.play"; }

    @Override
    public String getLabel() { return "Testsuite ausführen"; }

    @Override
    public void perform() {
        // Publish start message to StatusBar (via StatusTicker)
        ApplicationEventBus.getInstance()
                .publish(new StatusMessageEvent("▶ Starte Playback…", 1500, Severity.INFO));

        // Open runner tab and select it
        final RunnerPanel runnerPanel = new RunnerPanel();
        TestPlayerService.getInstance().registerLogger(runnerPanel.getLogger());

        tabbedPane.addTab("Runner", runnerPanel);
        final int tabIndex = tabbedPane.indexOfComponent(runnerPanel);
        tabbedPane.setTabComponentAt(tabIndex, createClosableTabHeader("Runner", tabbedPane, runnerPanel));
        tabbedPane.setSelectedComponent(runnerPanel);

        // Run tests in background
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    TestPlayerService.getInstance().runSuites();
                    // Notify finished
                    ApplicationEventBus.getInstance()
                            .publish(new StatusMessageEvent("✔ Playback fertig", 2000, Severity.INFO));
                } catch (Throwable t) {
                    // Notify error
                    String msg = (t.getMessage() == null) ? t.toString() : t.getMessage();
                    ApplicationEventBus.getInstance()
                            .publish(new StatusMessageEvent("Fehler im Playback: " + msg, 4000, Severity.ERROR));
                }
            }
        }, "WD4J-Runner").start();
    }

    /**
     * Create a closable tab header: "Runner   [×]".
     * Close stops playback, removes the tab, and publishes cancel status.
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
        close.setToolTipText("Playback abbrechen und Tab schließen");
        close.setForeground(new Color(200, 0, 0));
        Font f = close.getFont();
        close.setFont(f.deriveFont(Font.BOLD, f.getSize2D() + 1f));

        close.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                try {
                    TestPlayerService.getInstance().stopPlayback();
                } catch (Throwable ignore) {
                    // ignore
                }
                // Publish cancel message
                ApplicationEventBus.getInstance()
                        .publish(new StatusMessageEvent("⏹ Playback abgebrochen", 2000, Severity.WARN));

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
