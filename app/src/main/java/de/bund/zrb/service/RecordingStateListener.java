package de.bund.zrb.service;

/** Notify UI about start/stop state changes. */
public interface RecordingStateListener {
    void onRecordingStateChanged(boolean recording);
}
