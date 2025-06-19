package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.support.mapping.EnumWrapper;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDUrlPattern;
import de.bund.zrb.api.WDCommand;

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
