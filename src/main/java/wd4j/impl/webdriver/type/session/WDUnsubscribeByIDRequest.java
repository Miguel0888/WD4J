package wd4j.impl.webdriver.type.session;

import java.util.List;

public class WDUnsubscribeByIDRequest {
    private final List<WDSubscription> subscriptions;

    public WDUnsubscribeByIDRequest(List<WDSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<WDSubscription> getSubscriptions() {
        return subscriptions;
    }
}