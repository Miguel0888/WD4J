package de.bund.zrb.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Represent a neutral meta event that documents system reactions (navigation, ajax, load). */
public final class MetaEvent {

    public enum Kind {
        NETWORK_RESPONSE_STARTED,
        NETWORK_RESPONSE_FINISHED,
        NETWORK_REQUEST_FAILED,
        DOM_READY,
        PAGE_LOADED,
        NAVIGATION_STARTED,
        URL_FRAGMENT_CHANGED,
        HISTORY_CHANGED,
        NAVIGATION_COMMITTED,
        NAVIGATION_ABORTED,
        NAVIGATION_FAILED,
        NETWORK_REQUEST_SENT
    }

    private final Kind kind;
    private final long timestampMillis;
    private final Map<String, String> details;

    public MetaEvent(Kind kind, long timestampMillis, Map<String, String> details) {
        this.kind = kind;
        this.timestampMillis = timestampMillis;
        this.details = details != null ? new HashMap<String, String>(details) : new HashMap<String, String>();
    }

    public Kind getKind() { return kind; }

    public long getTimestampMillis() { return timestampMillis; }

    public Map<String, String> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    public static MetaEvent of(Kind kind) {
        return new MetaEvent(kind, System.currentTimeMillis(), null);
    }

    public static MetaEvent of(Kind kind, Map<String, String> details) {
        return new MetaEvent(kind, System.currentTimeMillis(), details);
    }
}
