package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.ui.RecorderListener;
import de.bund.zrb.service.RecordingSession;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

/**
 * Service responsible for collecting raw WebDriver events, converting them into
 * UI components and dispatching them to a {@link RecorderTabUi}. It manages
 * subscription via {@link WDUiAppender} and provides start/stop lifecycle.
 *
 * <p>The EventService does not interpret the events itself; it simply
 * wraps each incoming raw event into a simple Swing component (currently a
 * {@link JLabel}) and forwards it to the recorder UI. A future enhancement
 * could map events to richer components based on their type.</p>
 */
public final class EventService {
    /**
     * Container for a raw event consisting of its BiDi name and the raw payload.
     */
    private static final class RawEvent {
        final String name;
        final Object payload;
        RawEvent(String name, Object payload) {
            this.name = name;
            this.payload = payload;
        }
    }

    private final RecorderTabUi ui;
    private final RecordingSession session;
    private final BlockingQueue<RawEvent> queue = new LinkedBlockingQueue<RawEvent>();
    private final BiConsumer<String, Object> sink;
    private volatile boolean running;
    private Thread workerThread;
    private WDUiAppender appender;

    /**
     * Constructs a new EventService bound to the given session and UI. The service will
     * capture all raw events, record them on the associated {@link RecordingSession}
     * and dispatch visible events to the provided UI.
     *
     * @param ui      the recorder tab UI to deliver components to; must not be null
     * @param session the recording session this service belongs to; must not be null
     */
    public EventService(RecorderTabUi ui, RecordingSession session) {
        this.ui = Objects.requireNonNull(ui, "ui must not be null");
        this.session = Objects.requireNonNull(session, "session must not be null");
        // Sink: receive raw event and enqueue as RawEvent
        this.sink = new BiConsumer<String, Object>() {
            @Override
            public void accept(String name, Object event) {
                if (name != null && event != null) {
                    queue.offer(new RawEvent(name, event));
                }
            }
        };
    }

    /**
     * Starts listening for events on the given page. If a session is already
     * running this call has no effect. This method will subscribe for raw
     * events according to the enabled flags and begin dispatching them to the UI.
     *
     * @param page the Playwright page to attach to; must not be null
     */
    public synchronized void start(Page page) {
        if (running) return;
        if (page == null) throw new IllegalArgumentException("page must not be null");
        running = true;
        // Attach to the page using the current session flags; all raw events will be wired
        EnumMap<WDEventNames, Boolean> flags = session.getEventFlags();
        appender = WDUiAppender.attachToPage(page, sink, WDEventWiringConfig.defaults(), flags);
        startWorker();
    }

    /**
     * Starts listening for events on the given browser context. If a session is
     * already running this call has no effect.
     *
     * @param context the browser context to attach to; must not be null
     */
    public synchronized void start(BrowserContext context) {
        if (running) return;
        if (context == null) throw new IllegalArgumentException("context must not be null");
        running = true;
        EnumMap<WDEventNames, Boolean> flags = session.getEventFlags();
        appender = WDUiAppender.attachToContext(context, sink, WDEventWiringConfig.defaults(), flags);
        startWorker();
    }

    /**
     * Stops listening for events and cleans up resources. This method will
     * detach event listeners and stop the dispatch thread.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        // Detach from the Playwright page/context
        if (appender != null) {
            try {
                appender.detachAll();
            } catch (Throwable ignore) {
                // nothing
            }
            appender = null;
        }
        // Interrupt and join worker thread
        if (workerThread != null) {
            try {
                workerThread.interrupt();
                workerThread.join(200L);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }
        // Clear queue
        queue.clear();
    }

    /**
     * Internal helper to start the dispatch thread. Consumes raw events from
     * the internal queue, converts them into a Swing component, and posts
     * updates to the EDT via {@link SwingUtilities#invokeLater(Runnable)}.
     */
    private void startWorker() {
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        RawEvent ev = queue.take();
                        // Record the event on the session for later use (e.g. timing)
                        session.recordRawEvent(ev.name, ev.payload);
                        // Determine if the event should be displayed based on current flags
                        WDEventNames type = WDEventNames.fromName(ev.name);
                        boolean enabled = true;
                        if (type != null) {
                            EnumMap<WDEventNames, Boolean> flags = session.getEventFlags();
                            Boolean f = flags.get(type);
                            enabled = (f != null && f.booleanValue());
                        }
                        if (!enabled) {
                            continue;
                        }
                        // Create a simple label for the event; a richer component could be created here
                        JLabel label = new JLabel(String.valueOf(ev.payload));
                        // Annotate the component with the event name for filtering in the UI
                        label.putClientProperty("eventName", ev.name);
                        final String name = ev.name;
                        // Dispatch the component to the UI on the EDT
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                ui.appendEvent(name, label);
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Throwable t) {
                        // Ignore individual event processing errors
                    }
                }
            }
        }, "event-service-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Updates the enabled flags for event subscription. Calling this method
     * while the service is running will update the underlying appender's
     * subscriptions so that newly enabled events begin producing entries and
     * disabled events stop. The provided map is copied into an internal
     * EnumMap; missing entries will be treated as {@code false}.
     *
     * @param newFlags the updated flag map; may be null to disable all
     */
    public synchronized void updateFlags(Map<WDEventNames, Boolean> newFlags) {
        // The EventService delegates filtering to the session; however we still propagate
        // updates to the underlying appender in case it respects flag changes (e.g. to
        // avoid unnecessary wiring). Do not store flags locally.
        if (appender != null && newFlags != null) {
            appender.update(new EnumMap<WDEventNames, Boolean>(newFlags));
        }
    }
}