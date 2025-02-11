package wd4j.impl.webdriver.command.request.parameters.session.parameters;

import wd4j.impl.webdriver.type.session.Subscription;
import wd4j.impl.websocket.Command;

// aka.SubscriptionRequest
public class SubscribeParameters implements Command.Params {
    private final Subscription subscription;

    public SubscribeParameters(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }
}
