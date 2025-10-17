package de.bund.zrb.dto;

/** Represent one Growl notification coming from the page. */
public final class GrowlNotification {
    // Keep fields public for a simple data carrier (DTO)
    public String contextId;   // optional; try to resolve from message params if available
    public String type;        // "INFO" | "WARN" | "ERROR" | "FATAL"
    public String title;       // may be empty
    public String message;     // may be empty
    public long timestamp;     // epoch millis; 0 if unknown
}
