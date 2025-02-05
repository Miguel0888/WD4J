package wd4j.impl.webdriver.command.response.session;

import wd4j.impl.markerInterfaces.resultData.SessionResult;
import wd4j.impl.webdriver.type.session.Subscription;

public class SubscribeResult implements SessionResult {
    private final Subscription subscription;

    public SubscribeResult(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    @Override
    public String toString() {
        return "SessionSubscribeResult{" +
                "subscription=" + subscription +
                '}';
    }
}
