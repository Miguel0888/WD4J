package de.bund.zrb.ui.status;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.event.Severity;
import de.bund.zrb.ui.widgets.StatusBar;

import javax.swing.*;
import java.awt.event.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class StatusTicker {

    private static final int TICK_MS = 200;

    private static final class Item {
        final String text;
        final int durationMs;
        final Severity severity; // may be null
        Item(String text, int durationMs, Severity severity) {
            this.text = text;
            this.durationMs = durationMs;
            this.severity = severity;
        }
    }

    private static volatile StatusTicker INSTANCE;
    public static StatusTicker getInstance() {
        if (INSTANCE == null) {
            synchronized (StatusTicker.class) {
                if (INSTANCE == null) INSTANCE = new StatusTicker();
            }
        }
        return INSTANCE;
    }

    private final Queue<Item> queue = new ConcurrentLinkedQueue<Item>();
    private final Timer timer = new Timer(TICK_MS, new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) { onTick(); }
    });

    private volatile StatusBar statusBar;
    private volatile Item current;
    private volatile long deadlineMs;

    private StatusTicker() {
        timer.setRepeats(true);
        ApplicationEventBus.getInstance().subscribe(StatusMessageEvent.class, new Consumer<StatusMessageEvent>() {
            @Override public void accept(StatusMessageEvent ev) {
                StatusMessageEvent.Payload p = ev.getPayload();
                queue.add(new Item(p.getText(), p.getDurationMs(), p.getSeverity()));
                ensureRunning();
            }
        });
    }

    public synchronized void attach(StatusBar bar) {
        this.statusBar = bar;
        ensureRunning();
    }

    public synchronized void detach() {
        this.statusBar = null;
        if (timer.isRunning()) timer.stop();
        current = null;
        deadlineMs = 0L;
    }

    private synchronized void ensureRunning() {
        if (statusBar != null && !timer.isRunning()) timer.start();
        if (statusBar != null && current == null) advance();
    }

    private void onTick() {
        long now = System.currentTimeMillis();
        if (current == null || now >= deadlineMs) advance();
        if (current == null && queue.isEmpty() && timer.isRunning()) timer.stop();
    }

    private void advance() {
        current = queue.poll();
        if (current == null) { deadlineMs = 0L; return; }
        deadlineMs = System.currentTimeMillis() + current.durationMs;

        Icon icon = (current.severity == null) ? null : SeverityIconFactory.icon(current.severity, 14);
        setBar(current.text, icon);
    }

    private void setBar(final String text, final Icon icon) {
        final StatusBar bar = this.statusBar;
        if (bar == null) return;
        if (SwingUtilities.isEventDispatchThread()) {
            bar.setMessage(text, icon, true);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() { bar.setMessage(text, icon, true); }
            });
        }
    }
}
