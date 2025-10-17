package de.bund.zrb.dto;

public class GrowlNotification {
    public final String contextId; // Tab/BC
    public final String type;      // INFO|WARN|ERROR|FATAL
    public final String title;     // "Info", "Warnung", ...
    public final String message;   // eigentlicher Text
    public final long   timestamp; // ms since epoch

    public GrowlNotification(String contextId, String type, String title, String message, long timestamp) {
        this.contextId = contextId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    @Override public String toString() {
        return "[" + type + "] " + title + " — " + message + " (#" + contextId + ")";
    }
}
