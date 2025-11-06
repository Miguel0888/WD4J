package de.bund.zrb.ui.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Queue messages and display each for a fixed duration on the EDT. */
public final class StatusMessageQueue {

    /** Bridge to set the status on any widget. */
    public interface StatusTarget {
        /** Set message text. */
        void setStatus(String text);
    }

    private static final int TICK_MS = 200;

    private final Queue<String> pending = new ConcurrentLinkedQueue<String>();
    private final StatusTarget target;
    private final int durationMs;
    private final Timer timer;

    private volatile String current;
    private volatile long deadlineMs;
    private volatile boolean running;

    public StatusMessageQueue(StatusTarget target, int durationMs) {
        if (target == null) throw new IllegalArgumentException("target must not be null");
        if (durationMs <= 0) throw new IllegalArgumentException("durationMs must be > 0");
        this.target = target;
        this.durationMs = durationMs;
        this.timer = new Timer(TICK_MS, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                onTick();
            }
        });
        this.timer.setRepeats(true);
    }

    /** Enqueue a new message; start ticker if idle. */
    public void push(String msg) {
        if (msg == null) return;
        pending.add(msg);
        ensureStarted();
    }

    /** Start the ticker if not running. */
    public synchronized void start() {
        ensureStarted();
    }

    /** Stop the ticker and clear internal state (keeps pending queue intact). */
    public synchronized void stop() {
        running = false;
        timer.stop();
        current = null;
        deadlineMs = 0L;
    }

    /** Return whether the ticker is currently running. */
    public boolean isRunning() {
        return running;
    }

    private synchronized void ensureStarted() {
        if (!running) {
            running = true;
            if (!timer.isRunning()) timer.start();
        }
        if (current == null) advance(); // show first immediately
    }

    private void onTick() {
        long now = System.currentTimeMillis();
        if (current == null || now >= deadlineMs) {
            advance();
        }
        if (current == null && pending.isEmpty()) {
            // Nothing left → stop quietly
            stop();
        }
    }

    private void advance() {
        String next = pending.poll();
        if (next == null) {
            current = null;
            deadlineMs = 0L;
            return;
        }
        current = next;
        deadlineMs = System.currentTimeMillis() + durationMs;
        target.setStatus(current);
    }
}
