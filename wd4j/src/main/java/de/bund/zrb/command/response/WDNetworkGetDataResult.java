package de.bund.zrb.command.response;

import de.bund.zrb.api.markerInterfaces.WDResultData;
import de.bund.zrb.type.network.WDBytesValue;

/**
 * Result payload for network.getData.
 */
public class WDNetworkGetDataResult implements WDResultData {
    private final WDBytesValue bytes;

    public WDNetworkGetDataResult(WDBytesValue bytes) {
        this.bytes = bytes;
    }

    public WDBytesValue getBytes() {
        return bytes;
    }
}
