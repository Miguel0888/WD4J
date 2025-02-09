package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.type.network.Intercept;

public interface NetworkResult extends ResultData {
    class AddInterceptResult implements NetworkResult {
        Intercept intercept;

        public AddInterceptResult(Intercept intercept) {
            this.intercept = intercept;
        }

        public Intercept getIntercept() {
            return intercept;
        }
    }
}
