package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.api.WDCommand;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.network.WDHeader;

import java.util.List;

/**
 * Parameters for network.setExtraHeaders
 * Spec: https://www.w3.org/TR/webdriver-bidi/#command-network-setExtraHeaders
 */
public class SetExtraHeadersParameters implements WDCommand.Params {
    private final List<WDHeader> headers;
    private final List<WDBrowsingContext> contexts; // Optional
    private final List<WDUserContext> userContexts; // Optional

    public SetExtraHeadersParameters(List<WDHeader> headers) {
        this(headers, null, null);
    }

    public SetExtraHeadersParameters(List<WDHeader> headers,
                                     List<WDBrowsingContext> contexts,
                                     List<WDUserContext> userContexts) {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("headers must not be null or empty.");
        }
        this.headers = headers;
        this.contexts = contexts;
        this.userContexts = userContexts;
    }

    public List<WDHeader> getHeaders() {
        return headers;
    }

    public List<WDBrowsingContext> getContexts() {
        return contexts;
    }

    public List<WDUserContext> getUserContexts() {
        return userContexts;
    }
}
