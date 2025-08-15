package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;
import java.util.List;

/**
 * Receive updates from the recorder with the current ordered actions.
 * Implement in the UI component that renders the action list.
 * Always update Swing components on the EDT.
 */
public interface RecorderListener {

    /**
     * Receive the complete list of recorded actions for display.
     * Replace the current UI list with the given contents.
     */
    void onRecorderUpdated(List<TestAction> actions);
}
