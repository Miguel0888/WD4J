package de.bund.zrb.service;

import java.util.List;
import de.bund.zrb.model.TestAction;

public interface RecorderListener {
    /**
     * Wird aufgerufen, sobald sich die Liste der TestActions ändert.
     *
     * @param updatedActions Die aktuelle, gemergte Liste aller TestActions.
     */
    void onRecorderUpdated(List<TestAction> updatedActions);
}
