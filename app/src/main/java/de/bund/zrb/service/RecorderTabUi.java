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
}
