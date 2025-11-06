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
import java.util.concurrent.atomic.AtomicBoolean;

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
        // Status (ohne Icon)
        ApplicationEventBus.getInstance()
                .publish(new StatusMessageEvent("▶ Starte Playback…", 1500));

        // Runner-UI
        final RunnerPanel runnerPanel = new RunnerPanel();
        TestPlayerService.getInstance().registerLogger(runnerPanel.getLogger());

        // Pro Tab eigener Running-Status
        final AtomicBoolean running = new AtomicBoolean(false);

        tabbedPane.addTab("Runner", runnerPanel);
        final int tabIndex = tabbedPane.indexOfComponent(runnerPanel);
        tabbedPane.setTabComponentAt(tabIndex, createClosableTabHeader("Runner", tabbedPane, runnerPanel, running));
        tabbedPane.setSelectedComponent(runnerPanel);

        // Hintergrund-Thread starten
        new Thread(new Runnable() {
            @Override public void run() {
                running.set(true);
                try {
                    TestPlayerService.getInstance().runSuites();
                    // Erfolg
                    ApplicationEventBus.getInstance()
                            .publish(new StatusMessageEvent("✔ Playback fertig", 2000));
                } catch (Throwable t) {
                    // Fehler
                    String msg = (t.getMessage() == null) ? t.toString() : t.getMessage();
                    ApplicationEventBus.getInstance()
                            .publish(new StatusMessageEvent("Fehler im Playback: " + msg, 4000, Severity.ERROR));
                } finally {
                    // Wichtig: Status zurücksetzen, damit Close kein „abgebrochen“ mehr sendet
                    running.set(false);
                }
            }
        }, "WD4J-Runner").start();
    }

    /**
     * Create a closable tab header: "Runner   [×]".
     * Close stoppt nur, wenn noch läuft; Statusmeldung dann entsprechend.
     */
    private JComponent createClosableTabHeader(final String title,
                                               final JTabbedPane tabs,
                                               final Component tabContent,
                                               final AtomicBoolean running) {
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
        close.setToolTipText("Tab schließen" + " (stoppt Playback, falls noch aktiv)");
        close.setForeground(new Color(200, 0, 0));
        Font f = close.getFont();
        close.setFont(f.deriveFont(Font.BOLD, f.getSize2D() + 1f));

        close.addActionListener(new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                boolean wasRunning = running.get();
                if (wasRunning) {
                    try {
                        TestPlayerService.getInstance().stopPlayback();
                    } catch (Throwable ignore) { /* ignore */ }
                    // Nur dann Abbruchstatus senden, wenn wirklich noch lief
                    ApplicationEventBus.getInstance()
                            .publish(new StatusMessageEvent("⏹ Playback abgebrochen", 2000));
                }
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
