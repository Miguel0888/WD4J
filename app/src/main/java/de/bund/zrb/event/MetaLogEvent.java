package de.bund.zrb.event;

/** Carry a single formatted log line to the UI. */
public final class MetaLogEvent {
    private final String line;

    public MetaLogEvent(String line) {
        this.line = line;
    }

    public String getLine() {
        return line;
    }
}
