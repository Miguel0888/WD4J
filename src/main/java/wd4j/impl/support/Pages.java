package wd4j.impl.support;

import wd4j.api.Page;
import wd4j.impl.playwright.BrowserImpl;
import wd4j.impl.playwright.PageImpl;
import wd4j.impl.playwright.UserContextImpl;
import wd4j.impl.websocket.WDEventNames;
import wd4j.impl.dto.type.session.WDSubscriptionRequest;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Pages implements Iterable<PageImpl> {
    private final BrowserImpl browser;
    private final UserContextImpl userContext; // ToDo: Use this on every WebDriver command

    private final Map<String, PageImpl> pages = new ConcurrentHashMap<>();
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

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

    public Object getActivePageId() {
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

    /** 🔹 Enum für verschiedene Events */
    public enum EventType {
        BROWSING_CONTEXT_ADDED,  // 🔥 Aktualisiert die UI-Liste
        BROWSING_CONTEXT_REMOVED, // 🔥 Aktualisiert die UI-Liste
        ACTIVE_PAGE_CHANGED        // 🔥 Setzt das aktive Element in der UI
    }

    public void add(PageImpl page) {
        put(page.getBrowsingContextId(), page);
    }

    private void put(String contextId, PageImpl page) {
        PageImpl put = pages.put(contextId, page);
        if(put != null)
        { // avoid firing event if page was only updated / overwritten
            fireEvent(EventType.BROWSING_CONTEXT_ADDED, null, contextId);
            System.out.println("Added page: " + contextId);
        }
    }

    public void remove(String contextId) {
        if(contextId == null)
        {
            return;
        }
        PageImpl removedPage = pages.remove(contextId);
        if(removedPage != null)
        { // avoid firing event if page was not removed
            fireEvent(EventType.BROWSING_CONTEXT_REMOVED, null, contextId);
            System.out.println("Removed page: " + contextId);
        }
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

    public PageImpl getActivePage() {
        return get(activePageId);
    }

    public void setActivePageId(String contextId) {
        setActivePageId(contextId, false);
    }

    public void setActivePageId(String contextId, boolean isUiInitiated) {
        if(contextId == null)
        {
            return;
        }
        String oldPage = this.activePageId;
        this.activePageId = contextId;
        if(!Objects.equals(oldPage, contextId) && !isUiInitiated)
        {
            fireEvent(EventType.ACTIVE_PAGE_CHANGED, oldPage, contextId);
        }
        if(isUiInitiated)
        {
            browser.getWebDriver().browsingContext().activate(contextId);
        }
    }

    /** 🔹 Einheitliche Methode zum Feuern von Events */
    private void fireEvent(EventType eventType, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(eventType.name(), oldValue, newValue);
    }

    public void addListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
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
