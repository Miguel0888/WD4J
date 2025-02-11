package wd4j.impl.webdriver.command.request.parameters.session.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.session.Subscription;
import wd4j.impl.websocket.Command;

import java.util.List;

public interface UnsubscribeParameters extends Command.Params {

    public class UnsubscribeByAttributesRequestParams extends wd4j.impl.webdriver.type.session.UnsubscribeByAttributesRequest implements UnsubscribeParameters {
        public UnsubscribeByAttributesRequestParams(List<String> events) {
            super(events, null);
        }

        public UnsubscribeByAttributesRequestParams(List<String> events, List<BrowsingContext> contexts) {
            super(events, contexts);
        }
    }

    public class UnsubscribeByIDRequestParams extends wd4j.impl.webdriver.type.session.UnsubscribeByIDRequest implements UnsubscribeParameters {
        public UnsubscribeByIDRequestParams(List<Subscription> subscriptions) {
            super(subscriptions);
        }
    }
}
