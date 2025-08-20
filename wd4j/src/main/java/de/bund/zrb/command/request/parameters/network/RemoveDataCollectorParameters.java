package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.api.WDCommand;
import de.bund.zrb.type.network.WDCollector;

/**
 * Parameters for network.removeDataCollector
 * Spec: https://www.w3.org/TR/webdriver-bidi/#command-network-removeDataCollector
 */
public class RemoveDataCollectorParameters implements WDCommand.Params {
    private final WDCollector collector;

    public RemoveDataCollectorParameters(WDCollector collector) {
        if (collector == null) {
            throw new IllegalArgumentException("collector must not be null.");
        }
        this.collector = collector;
    }

    public WDCollector getCollector() {
        return collector;
    }
}
