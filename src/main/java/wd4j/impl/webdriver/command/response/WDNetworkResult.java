package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.network.WDIntercept;

public interface WDNetworkResult extends WDResultData {
    class AddInterceptWDNetworkResult implements WDNetworkResult {
        WDIntercept intercept;

        public AddInterceptWDNetworkResult(WDIntercept intercept) {
            this.intercept = intercept;
        }

        public WDIntercept getIntercept() {
            return intercept;
        }
    }
}
