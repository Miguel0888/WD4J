package wd4j.impl.webdriver.type.session;

import java.util.List;

public class UnsubscribeByIDRequest {
    private final List<Subscription> subscriptions;

    public UnsubscribeByIDRequest(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}