package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.helper.EmptyParameters;
import wd4j.impl.webdriver.command.request.parameters.session.parameters.NewParameters;
import wd4j.impl.webdriver.command.request.parameters.session.parameters.UnsubscribeParameters;
import wd4j.impl.webdriver.type.session.*;

import java.util.List;

public class SessionRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Status extends CommandImpl<EmptyParameters> implements CommandData {
        public Status() {
            super("session.status", new EmptyParameters());
        }
    }


    public static class New extends CommandImpl<NewParameters> implements CommandData {
        public New(String browserName) {
            super("session.new", new NewParameters(new CapabilitiesRequest(new CapabilityRequest(
                    null, browserName, null,
                    null, null, null))));
        }
        public New(CapabilitiesRequest capabilities) {
            super("session.new", new NewParameters(capabilities));
        }
    }

    public static class End extends CommandImpl<EmptyParameters> implements CommandData {
        public End() {
            super("session.delete", new EmptyParameters());
        }
    }

    public static class Subscribe extends CommandImpl<SubscriptionRequest> implements CommandData {
        public Subscribe(List<String> events) {
            super("session.subscribe", new SubscriptionRequest(events));
        }
    }

    public static class Unsubscribe extends CommandImpl<UnsubscribeParameters> implements CommandData {
        public Unsubscribe(List<String> events) {
            super("session.unsubscribe", new UnsubscribeParameters.UnsubscribeByAttributesRequestParams(events));
        }
        public Unsubscribe(UnsubscribeParameters unsubscribeParameters) {
            super("session.unsubscribe", unsubscribeParameters);
        }
    }
}