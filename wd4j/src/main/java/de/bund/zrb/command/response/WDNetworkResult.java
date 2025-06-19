package de.bund.zrb.command.response;

import de.bund.zrb.api.markerInterfaces.WDResultData;
import de.bund.zrb.type.network.WDIntercept;

public interface WDNetworkResult extends WDResultData {
    class AddInterceptResult implements WDNetworkResult {
        WDIntercept intercept;

        public AddInterceptResult(WDIntercept intercept) {
            this.intercept = intercept;
        }

        public WDIntercept getIntercept() {
            return intercept;
        }
    }
}
