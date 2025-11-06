package de.bund.zrb.service;

/** Signal cooperative cancellation during playback. */
public final class StopRequestedException extends RuntimeException {
    public StopRequestedException() { super("Playback stop requested"); }
}
