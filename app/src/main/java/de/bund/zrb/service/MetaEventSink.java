package de.bund.zrb.service;

/** Abstrahiere, wohin Meta-Logs gehen (console, bus, file, ...). */
public interface MetaEventSink {
    /** Append a single, already formatted line. */
    void append(String line);
}
