package de.bund.zrb.impl.webdriver.command.response;

import de.bund.zrb.impl.markerInterfaces.WDResultData;
import de.bund.zrb.impl.webdriver.type.network.WDIntercept;

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
