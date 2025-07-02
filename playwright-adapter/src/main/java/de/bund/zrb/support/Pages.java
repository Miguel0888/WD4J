package de.bund.zrb.support;

import com.microsoft.playwright.Page;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.UserContextImpl;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.type.session.WDSubscriptionRequest;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Pages implements Iterable<PageImpl> {
    private final BrowserImpl browser;
    private final UserContextImpl userContext; // ToDo: Use this on every WebDriver command

    private final Map<String, PageImpl> pages = new ConcurrentHashMap<>();

    private String activePageId;

    public Pages(BrowserImpl browser) {
        this.browser = browser;
        this.userContext = null;
    }

    public Pages(BrowserImpl browser, UserContextImpl userContext) {
        this.browser = browser;
        this.userContext = userContext;
    }

    public Set<String> keySet() {
        return pages.keySet();
    }

    public String getActivePageId() {
        if( activePageId == null) {
            return pages.entrySet().stream().findFirst().get().getValue().getBrowsingContextId();
        }
        return activePageId;
    }

    public void clear() {
        pages.clear();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<PageImpl> iterator() {
        return pages.values().iterator();
    }

    public List<? super PageImpl> asList() {
        return new ArrayList<>(pages.values());
    }

    public void add(PageImpl page) {
        String contextId = page.getBrowsingContextId();
        PageImpl put = pages.put(contextId, page);
    }

    public void remove(String contextId) {
        if(contextId == null)
        {
            return;
        }
        PageImpl removedPage = pages.remove(contextId);
    }

    public Set<String> getContextIds() {
        return Collections.unmodifiableSet(pages.keySet());
    }

    public PageImpl get(String contextId) {
        if(contextId == null)
        {
            return null;
        }
        return pages.get(contextId);
    }

    public void onCreated(Consumer<Page> handler) {
        if (handler != null) {
            WDSubscriptionRequest subscriptionRequest = new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), null, null);
            browser.getWebDriver().addEventListener(subscriptionRequest, handler);
        }
    }

    public void offCreated(Consumer<Page> createdHandler) {
        if (createdHandler != null) {
            browser.getWebDriver().removeEventListener(WDEventNames.CONTEXT_CREATED.getName(), null, createdHandler);
        }
    }
}
