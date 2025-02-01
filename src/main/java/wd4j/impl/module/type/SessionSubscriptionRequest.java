package wd4j.impl.module.type;

import java.util.List;

public class SessionSubscriptionRequest {
    private final List<SessionSubscription> subscriptions;

    public SessionSubscriptionRequest(List<SessionSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            throw new IllegalArgumentException("Subscriptions must not be null or empty.");
        }
        this.subscriptions = subscriptions;
    }

    public List<SessionSubscription> getSubscriptions() {
        return subscriptions;
    }
}