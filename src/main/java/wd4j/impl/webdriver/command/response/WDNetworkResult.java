package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.network.WDIntercept;

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
