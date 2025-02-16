package wd4j.impl.webdriver.type.session;

import java.util.List;

public class WDUnsubscribeByIDRequest {
    private final List<WDSubscription> WDSubscriptions;

    public WDUnsubscribeByIDRequest(List<WDSubscription> WDSubscriptions) {
        this.WDSubscriptions = WDSubscriptions;
    }

    public List<WDSubscription> getSubscriptions() {
        return WDSubscriptions;
    }
}