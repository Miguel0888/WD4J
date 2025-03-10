package wd4j.impl.support;

import wd4j.impl.playwright.PageImpl;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Pages {
    private final Map<String, PageImpl> pages = new ConcurrentHashMap<>();
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private String activePageId;

    public Set<String> keySet() {
        return pages.keySet();
    }

    public Object getActivePageId() {
        return activePageId;
    }

    /** 🔹 Enum für verschiedene Events */
    public enum EventType {
        BROWSING_CONTEXT_ADDED,  // 🔥 Aktualisiert die UI-Liste
        BROWSING_CONTEXT_REMOVED, // 🔥 Aktualisiert die UI-Liste
        ACTIVE_PAGE_CHANGED        // 🔥 Setzt das aktive Element in der UI
    }

    public void put(String contextId, PageImpl page) {
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
        setActivePageId(contextId, true);
    }

    public void setActivePageId(String contextId, boolean fireEvent) {
        if(contextId == null)
        {
            return;
        }
        String oldPage = this.activePageId;
        this.activePageId = contextId;
        if(!Objects.equals(oldPage, contextId) && fireEvent)
        {
            fireEvent(EventType.ACTIVE_PAGE_CHANGED, oldPage, contextId);
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
}
