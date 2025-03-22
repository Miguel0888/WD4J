package wd4j.impl.dto.command.request.parameters.session.parameters;

import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;
import wd4j.impl.dto.type.session.WDSubscription;
import wd4j.impl.dto.type.session.WDUnsubscribeByAttributesRequest;
import wd4j.impl.dto.type.session.WDUnsubscribeByIDRequest;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public interface UnsubscribeParameters extends WDCommand.Params {

    public class WDUnsubscribeByAttributesRequestParams extends WDUnsubscribeByAttributesRequest implements UnsubscribeParameters {
        public WDUnsubscribeByAttributesRequestParams(List<String> events) {
            super(events, null);
        }

        public WDUnsubscribeByAttributesRequestParams(List<String> events, List<WDBrowsingContext> contexts) {
            super(events, contexts);
        }
    }

    // ToDo: Not supported in Firefox yet?
    public class WDUnsubscribeByIDRequestParams extends WDUnsubscribeByIDRequest implements UnsubscribeParameters {
        public WDUnsubscribeByIDRequestParams(List<WDSubscription> WDSubscriptions) {
            super(WDSubscriptions);
        }
    }
}
