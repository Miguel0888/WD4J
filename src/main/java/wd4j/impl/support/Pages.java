package wd4j.impl.support;

import wd4j.impl.playwright.PageImpl;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Pages {
    private final Map<String, PageImpl> pages = new ConcurrentHashMap<>();
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private PageImpl activePage;

    public String[] keySet() {
        return pages.keySet().toArray(new String[0]);
    }

    /** 🔹 Enum für verschiedene Events */
    public enum EventType {
        BROWSING_CONTEXT,  // 🔥 Aktualisiert die UI-Liste
        ACTIVE_PAGE        // 🔥 Setzt das aktive Element in der UI
    }

    public void put(String contextId, PageImpl page) {
        pages.put(contextId, page);
        fireEvent(EventType.BROWSING_CONTEXT, null, getContextIds());
    }

    public void remove(String contextId) {
        PageImpl removedPage = pages.remove(contextId);
        fireEvent(EventType.BROWSING_CONTEXT, null, getContextIds());

        if (removedPage != null && removedPage.equals(activePage)) {
            setActivePage((String) null);
        }
    }

    public Set<String> getContextIds() {
        return Collections.unmodifiableSet(pages.keySet());
    }

    public PageImpl get(String contextId) {
        return pages.get(contextId);
    }

    public PageImpl getActivePage() {
        return activePage;
    }

    public void setActivePage(String contextId) {
        System.out.println("+++++++++++++++++++++++++++++++  Setting active page to: " + contextId);
        setActivePage(pages.get(contextId));
    }

    public void setActivePage(PageImpl newPage) {
        PageImpl oldPage = this.activePage;
        this.activePage = newPage;
        fireEvent(EventType.ACTIVE_PAGE, oldPage, newPage);
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
