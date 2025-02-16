package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.network.WDUrlPattern;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class AddInterceptParameters implements WDCommand.Params {
    private final List<InterceptPhase> phases;
    private final List<WDBrowsingContext> contexts; //optional
    private final List<WDUrlPattern> WDUrlPatterns; //optional

    public AddInterceptParameters(List<InterceptPhase> phases) {
        this(phases, null, null);
    }

    public AddInterceptParameters(List<InterceptPhase> phases, List<WDBrowsingContext> contexts, List<WDUrlPattern> WDUrlPatterns) {
        this.phases = phases;
        this.contexts = contexts;
        this.WDUrlPatterns = WDUrlPatterns;
    }

    public List<InterceptPhase> getPhases() {
        return phases;
    }

    public List<WDBrowsingContext> getContexts() {
        return contexts;
    }

    public List<WDUrlPattern> getUrlPatterns() {
        return WDUrlPatterns;
    }

    public enum InterceptPhase implements EnumWrapper {
        BEFORE_REQUEST_SENT("beforeRequestSent"),
        RESPONSE_STARTED("responseStarted"),
        AUTH_REQUIRED("authRequired");

        private final String value;

        InterceptPhase(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}
