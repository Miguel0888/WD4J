package de.bund.zrb.ui.status;

import javax.swing.*;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Einfache Queue für Statusleisten-Nachrichten mit Mindestanzeigedauer.
 * - Thread-safe für Aufrufer.
 * - Sink wird auf dem EDT aufgerufen.
 */
public final class StatusBarEventQueue {

    private static final StatusBarEventQueue INSTANCE = new StatusBarEventQueue();
    public static StatusBarEventQueue getInstance() { return INSTANCE; }

    private final Object lock = new Object();
    private final Queue<String> q = new ArrayDeque<>();

    private volatile Consumer<String> sink = null;
    private volatile long minDisplayMillis = 3000;

    private volatile boolean showing = false;

    private StatusBarEventQueue() {}

    /** Ziel-Senke setzen (z.B. StatusBarManager::setMessage). */
    public void setSink(Consumer<String> sink) {
        this.sink = sink;
    }

    /** Mindestanzeigedauer pro Nachricht (ms). */
    public void setMinDisplayMillis(long ms) {
        this.minDisplayMillis = Math.max(0, ms);
    }

    /** Nachricht einreihen. Null/leer wird ignoriert. */
    public void post(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;
        synchronized (lock) {
            q.offer(msg.trim());
            if (!showing) {
                showing = true;
                showNextOnEdt();
            }
        }
    }

    // ---- intern ----
    private void showNextOnEdt() {
        SwingUtilities.invokeLater(() -> {
            final String next;
            synchronized (lock) {
                next = q.poll();
                if (next == null) {
                    showing = false;
                    return;
                }
            }

            Consumer<String> s = this.sink;
            if (s != null) {
                try {
                    s.accept(next);
                } catch (Throwable ignored) {}
            }

            // Nach der Mindestzeit die nächste Nachricht zeigen
            new Timer((int) Math.max(0, minDisplayMillis), e -> {
                ((Timer) e.getSource()).stop();
                synchronized (lock) {
                    if (q.isEmpty()) {
                        showing = false;
                    } else {
                        showNextOnEdt();
                    }
                }
            }) {{
                setRepeats(false);
            }}.start();
        });
    }
}
