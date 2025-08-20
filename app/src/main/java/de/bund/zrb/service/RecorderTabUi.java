package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.RecorderListener;

import java.util.List;

/** Minimal contract the service expects from a recorder tab UI. */
public interface RecorderTabUi extends RecorderListener, RecordingStateListener {
    /**
     * Returns the username associated with this recorder UI instance.
     */
    String getUsername();

    /**
     * Indicates whether the UI is currently visible and active.
     */
    boolean isVisibleActive();

    /**
     * Updates the list of recorded actions to display.
     *
     * @param actions the new list of actions
     */
    void setActions(List<TestAction> actions);

    /**
     * Notifies the UI that the recording state has changed.
     *
     * @param recording {@code true} if recording is active
     */
    void setRecordingUiState(boolean recording);

    /**
     * Appends a meta-information string to the log. Existing implementations may override this
     * method to provide a textual fallback if component based logging is not yet implemented.
     *
     * @param line the text to append
     */
    void appendMeta(String line);

    /**
     * Appends a visual component to the log. This method should be used for rich UI logging
     * (e.g. coloured badges and summaries) instead of plain strings. Implementations that do
     * not yet support components may delegate to {@link #appendMeta(String)} by converting
     * the component to a string via {@link java.awt.Component#toString()}.
     *
     * @param component the Swing component representing the log entry
     */
    default void appendMeta(javax.swing.JComponent component) {
        // Fallback to string representation if not overridden
        appendMeta(component == null ? "null" : component.toString());
    }

    // --- String-based variants (legacy) ---
    /**
     * Appends an event to the log using its {@code toString()} representation. This method is
     * retained for backward compatibility with existing code paths. New code should provide
     * component-based log entries via {@link #appendMeta(javax.swing.JComponent)}.
     *
     * @param event the event object, may be {@code null}
     */
    default void appendEvent(Object event) {
        appendMeta(event == null ? "null" : event.toString());
    }

    /**
     * Appends an event with a BiDi event name prefix using the event's {@code toString()}
     * representation. This method is retained for backward compatibility. New code should
     * generate a Swing component representing the event and call
     * {@link #appendEvent(String, javax.swing.JComponent)} instead.
     *
     * @param bidiEventName the BiDi event name (may be {@code null})
     * @param event         the event object, may be {@code null}
     */
    default void appendEvent(String bidiEventName, Object event) {
        String e = (event == null ? "null" : event.toString());
        appendMeta((bidiEventName == null ? "" : bidiEventName + "  ") + e);
    }

    // --- Component-based variants (new) ---
    /**
     * Appends a rendered event component to the log. The provided component should encapsulate
     * the visual representation of the event (e.g. coloured badge, summary text). Implementations
     * may override this method to insert the component into the UI. By default this delegates
     * to {@link #appendMeta(javax.swing.JComponent)}.
     *
     * @param component the Swing component representing the log entry
     */
    default void appendEvent(javax.swing.JComponent component) {
        appendMeta(component);
    }

    /**
     * Appends a rendered event component with an associated BiDi event name. Implementations
     * may choose to display the BiDi name as part of the visual component or ignore it if the
     * component already contains the necessary context. By default this delegates to
     * {@link #appendEvent(javax.swing.JComponent)} and ignores the name.
     *
     * @param bidiEventName the BiDi event name (may be {@code null})
     * @param component     the Swing component representing the log entry
     */
    default void appendEvent(String bidiEventName, javax.swing.JComponent component) {
        // The default implementation does not render the name separately; override if needed
        appendEvent(component);
    }

    // --- JSON variants (legacy) ---
    /**
     * Appends an event serialized to JSON. This method is retained for compatibility and will
     * delegate to {@link #appendMeta(String)}. New code should use component-based logging.
     *
     * @param event the event object to serialize
     */
    default void appendEventJson(Object event) {
        try {
            appendMeta(JsonHolder.GSON.toJson(event));
        } catch (Throwable t) {
            // Fallback falls etwas nicht serialisierbar ist
            appendEvent(event);
        }
    }

    /**
     * Appends an event serialized to JSON with an associated BiDi event name. This method is
     * retained for compatibility and will delegate to {@link #appendMeta(String)}. New code
     * should use component-based logging.
     *
     * @param bidiEventName the BiDi event name (may be {@code null})
     * @param event         the event object to serialize
     */
    default void appendEventJson(String bidiEventName, Object event) {
        try {
            String json = JsonHolder.GSON.toJson(event);
            appendMeta((bidiEventName == null ? "" : bidiEventName + "  ") + json);
        } catch (Throwable t) {
            appendEvent(bidiEventName, event);
        }
    }

    /**
     * Holder for a single Gson instance (thread-safe). Used to serialize event objects to JSON
     * when falling back to string-based logging. Component-based logging should not rely on JSON.
     */
    final class JsonHolder {
        static final com.google.gson.Gson GSON =
                new com.google.gson.GsonBuilder()
                        .disableHtmlEscaping()
                        .serializeNulls()
                        // .setPrettyPrinting() // uncomment for pretty-printed JSON if required
                        .create();
        private JsonHolder() {}
    }
}
