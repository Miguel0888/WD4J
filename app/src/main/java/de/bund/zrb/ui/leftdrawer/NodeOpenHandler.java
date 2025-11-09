package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.ui.TestNode;

/**
 * Abstrakter Öffnungs-Handler für LeftDrawer-Nodes.
 * Dient dazu, UI-Listener schlank zu halten und die Öffnungslogik zentral zu kapseln.
 */
public interface NodeOpenHandler {
    /** Öffnet den aktuell gewählten Knoten in einem persistenten, neuen Tab. */
    void openInNewTab(TestNode node);
}

