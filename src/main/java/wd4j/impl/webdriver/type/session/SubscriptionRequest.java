package wd4j.impl.webdriver.type.session;

import java.util.List;

public class SubscriptionRequest {
    private final List<Subscription> subscriptions;

    public SubscriptionRequest(List<Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            throw new IllegalArgumentException("Subscriptions must not be null or empty.");
        }
        this.subscriptions = subscriptions;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}