package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.api.WDCommand;
import de.bund.zrb.type.network.WDCollector;
import de.bund.zrb.type.network.WDDataType;
import de.bund.zrb.type.network.WDRequest;

/**
 * Parameters for network.getData
 * Spec: https://www.w3.org/TR/webdriver-bidi/#command-network-getData
 */
public class GetDataParameters implements WDCommand.Params {
    private final WDDataType dataType;
    private final WDCollector collector; // Optional
    private final Boolean disown; // Optional, defaults to false
    private final WDRequest request;

    public GetDataParameters(WDDataType dataType, WDRequest request) {
        this(dataType, null, null, request);
    }

    public GetDataParameters(WDDataType dataType, WDCollector collector, Boolean disown, WDRequest request) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType must not be null.");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }
        this.dataType = dataType;
        this.collector = collector;
        this.disown = disown;
        this.request = request;
    }

    public WDDataType getDataType() {
        return dataType;
    }

    public WDCollector getCollector() {
        return collector;
    }

    public Boolean getDisown() {
        return disown;
    }

    public WDRequest getRequest() {
        return request;
    }
}
