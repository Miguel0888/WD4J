package wd4j.impl.support;

import com.google.gson.JsonObject;
import wd4j.impl.manager.WDSessionManager;
import wd4j.impl.webdriver.type.session.WDSubscription;
import wd4j.impl.webdriver.type.session.WDSubscriptionRequest;

import java.util.function.Consumer;

public interface EventDispatcher {
    void processEvent(JsonObject jsonMessage);

    <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> listener, WDSessionManager sessionManager);

    <T> void removeEventListener(String eventType, Consumer<T> listener, WDSessionManager sessionManager);

    <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener, WDSessionManager sessionManager);

    // ToDo: Not supported yet
    <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener, WDSessionManager sessionManager);

    Object mapEvent(String eventType, JsonObject json);
}
