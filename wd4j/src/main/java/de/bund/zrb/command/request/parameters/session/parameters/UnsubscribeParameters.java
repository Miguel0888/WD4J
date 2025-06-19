package de.bund.zrb.command.request.parameters.session.parameters;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDUnsubscribeByAttributesRequest;
import de.bund.zrb.type.session.WDUnsubscribeByIDRequest;
import de.bund.zrb.api.WDCommand;

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
