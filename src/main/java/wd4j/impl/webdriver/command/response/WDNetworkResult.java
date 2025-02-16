package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.network.WDIntercept;

public interface WDNetworkResult extends WDResultData {
    class AddInterceptWDNetworkResult implements WDNetworkResult {
        WDIntercept WDIntercept;

        public AddInterceptWDNetworkResult(WDIntercept WDIntercept) {
            this.WDIntercept = WDIntercept;
        }

        public WDIntercept getIntercept() {
            return WDIntercept;
        }
    }
}
