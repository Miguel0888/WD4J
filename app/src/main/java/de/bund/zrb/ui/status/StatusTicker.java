package de.bund.zrb.ui.status;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.ui.widgets.StatusBar;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/** Display status messages sequentially with fixed per-item duration. */
public final class StatusTicker {

    private static final int TICK_MS = 200;

    private static final class Item {
        final String text;
        final int durationMs;
        Item(String text, int durationMs) {
            this.text = text;
            this.durationMs = durationMs <= 0 ? 3000 : durationMs;
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
    private final Timer timer;

    private volatile StatusBar statusBar; // injected
    private volatile Item current;
    private volatile long deadlineMs;

    private final Consumer<de.bund.zrb.event.ApplicationEvent<?>> busListener = new Consumer<de.bund.zrb.event.ApplicationEvent<?>>() {
        @Override public void accept(de.bund.zrb.event.ApplicationEvent<?> ev) {
            StatusMessageEvent.Payload p = ((StatusMessageEvent) ev).getPayload();
            queue.add(new Item(p.getText(), p.getDurationMs()));
            ensureRunning();
        }
    };

    private StatusTicker() {
        this.timer = new Timer(TICK_MS, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { onTick(); }
        });
        this.timer.setRepeats(true);

        // Subscribe typed to StatusMessageEvent
        ApplicationEventBus.getInstance().subscribe(StatusMessageEvent.class, new Consumer<StatusMessageEvent>() {
            @Override public void accept(StatusMessageEvent ev) {
                StatusMessageEvent.Payload p = ev.getPayload();
                queue.add(new Item(p.getText(), p.getDurationMs()));
                ensureRunning();
            }
        });
    }

    /** Attach StatusBar when MainWindow is ready. */
    public synchronized void attach(StatusBar bar) {
        this.statusBar = bar;
        ensureRunning(); // start if items already queued
    }

    /** Optional: stop timer (e.g., on windowClosing). */
    public synchronized void detach() {
        this.statusBar = null;
        if (timer.isRunning()) timer.stop();
        current = null;
        deadlineMs = 0L;
    }

    private synchronized void ensureRunning() {
        if (statusBar != null && !timer.isRunning()) {
            timer.start();
        }
        if (statusBar != null && current == null) {
            advance();
        }
    }

    private void onTick() {
        long now = System.currentTimeMillis();
        if (current == null || now >= deadlineMs) {
            advance();
        }
        if (current == null && queue.isEmpty()) {
            if (timer.isRunning()) timer.stop();
        }
    }

    private void advance() {
        current = queue.poll();
        if (current == null) {
            deadlineMs = 0L;
            return;
        }
        deadlineMs = System.currentTimeMillis() + current.durationMs;
        setBarText(current.text);
    }

    private void setBarText(final String text) {
        final StatusBar bar = this.statusBar;
        if (bar == null) return;
        if (SwingUtilities.isEventDispatchThread()) {
            bar.setMessage(text);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() { bar.setMessage(text); }
            });
        }
    }
}
