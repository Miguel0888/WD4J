package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.ui.RecorderListener;

import java.util.List;

/** Minimal contract the service expects from a recorder tab UI. */
public interface RecorderTabUi extends RecorderListener, RecordingStateListener {
    String getUsername();
    boolean isVisibleActive();
    void setActions(List<TestAction> actions);
    void setRecordingUiState(boolean recording);

    void appendMeta(String line);

    // --- String-Varianten (bestehend) ---
    default void appendEvent(Object event) {
        appendMeta(event == null ? "null" : event.toString());
    }
    default void appendEvent(String bidiEventName, Object event) {
        String e = (event == null ? "null" : event.toString());
        appendMeta((bidiEventName == null ? "" : bidiEventName + "  ") + e);
    }

    // --- JSON-Varianten (neu) ---
    default void appendEventJson(Object event) {
        try {
            appendMeta(JsonHolder.GSON.toJson(event));
        } catch (Throwable t) {
            // Fallback falls etwas nicht serialisierbar ist
            appendEvent(event);
        }
    }
    default void appendEventJson(String bidiEventName, Object event) {
        try {
            String json = JsonHolder.GSON.toJson(event);
            appendMeta((bidiEventName == null ? "" : bidiEventName + "  ") + json);
        } catch (Throwable t) {
            appendEvent(bidiEventName, event);
        }
    }

    // Kleiner Holder für einen einzigen Gson (thread-sicher)
    final class JsonHolder {
        static final com.google.gson.Gson GSON =
                new com.google.gson.GsonBuilder()
                        .disableHtmlEscaping()
                        .serializeNulls()
                        //.setPrettyPrinting() // auskommentieren, wenn du kompakt willst
                        .create();
        private JsonHolder() {}
    }
}
