package wd4j.impl.webdriver.command.response.network;

import wd4j.impl.markerInterfaces.resultData.NetworkResult;
import wd4j.impl.webdriver.type.network.Intercept;

public class AddInterceptResult implements NetworkResult {
    Intercept intercept;

    public AddInterceptResult(Intercept intercept) {
        this.intercept = intercept;
    }

    public Intercept getIntercept() {
        return intercept;
    }
}
