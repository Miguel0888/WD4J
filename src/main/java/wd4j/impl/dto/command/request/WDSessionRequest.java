package wd4j.impl.dto.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.dto.command.request.helper.WDCommandImpl;
import wd4j.impl.dto.command.request.helper.WDEmptyParameters;
import wd4j.impl.dto.command.request.parameters.session.parameters.NewParameters;
import wd4j.impl.dto.command.request.parameters.session.parameters.UnsubscribeParameters;
import wd4j.impl.dto.type.session.*;

import java.util.List;

public class WDSessionRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Status extends WDCommandImpl<WDEmptyParameters> implements WDCommandData {
        public Status() {
            super("session.status", new WDEmptyParameters());
        }
    }


    public static class New extends WDCommandImpl<NewParameters> implements WDCommandData {
        public New(String browserName) {
            super("session.new", new NewParameters(new WDCapabilitiesRequest(new WDCapabilityRequest(
                    null, browserName, null,
                    null, null, null))));
        }
        public New(WDCapabilitiesRequest capabilities) {
            super("session.new", new NewParameters(capabilities));
        }
    }

    public static class End extends WDCommandImpl<WDEmptyParameters> implements WDCommandData {
        public End() {
            super("session.delete", new WDEmptyParameters());
        }
    }

    public static class Subscribe extends WDCommandImpl<WDSubscriptionRequest> implements WDCommandData {
        @Deprecated
        public Subscribe(List<String> events) {
            super("session.subscribe", new WDSubscriptionRequest(events));
        }
        public Subscribe(WDSubscriptionRequest subscriptionRequest) {
            super("session.subscribe", subscriptionRequest);
        }
    }

    public static class Unsubscribe extends WDCommandImpl<UnsubscribeParameters> implements WDCommandData {
        public Unsubscribe(List<String> events) {
            super("session.unsubscribe", new UnsubscribeParameters.WDUnsubscribeByAttributesRequestParams(events));
        }
        public Unsubscribe(UnsubscribeParameters unsubscribeParameters) {
            super("session.unsubscribe", unsubscribeParameters);
        }
    }
}