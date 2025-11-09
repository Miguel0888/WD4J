package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.giveneditor.CaseScopeEditorTab;
import de.bund.zrb.ui.giveneditor.RootScopeEditorTab;
import de.bund.zrb.ui.giveneditor.SuiteScopeEditorTab;
import de.bund.zrb.ui.leftdrawer.NodeOpenHandler;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TabManager verwaltet genau EIN Preview-Tab (flüchtig) und beliebig viele persistente Tabs.
 * Persistente Tabs bekommen einen ClosableTabHeader. Preview-Tab nicht.
 * Unterscheidung erfolgt NICHT über einen Titelpräfix, sondern über interne Metadaten.
 */
public class TabManager implements NodeOpenHandler {
    /** Interne Repräsentation eines Tabs. */
    private static class TabEntry {
        Component component;
        Object modelRef;
        String id; // eindeutige ID (Model-UUID oder synthetisch)
        boolean persistent; // false = Preview
        String title;
    }

    private final JTabbedPane editorTabs;
    private final Component parent;
    // Map für persistente Tabs: ID -> TabEntry
    private final Map<String, TabEntry> persistentTabs = new LinkedHashMap<>();
    // Preview-Tab separat
    private volatile TabEntry previewEntry; // volatile für spätere Thread-Sicherheit
    private volatile String previewId;

    public TabManager(Component parent, JTabbedPane editorTabs) {
        this.parent = parent;
        this.editorTabs = editorTabs;
    }

    // ========================= Öffentliche API =========================

    /** Zeige Node im Preview-Tab oder fokussiere bestehenden persistenten Tab. */
    public void showInPreview(TestNode node) {
        if (node == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;
        String id = extractId(ref);
        // Persistenter Tab vorhanden? -> Fokus statt Preview
        if (id != null && focusPersistentTab(id)) return;
        createOrUpdatePreview(id, ref);
    }

    /** Öffnet einen persistenten Tab oder fokussiert existierenden. */
    @Override
    public void openInNewTab(TestNode node) {
        if (node == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;
        String id = extractId(ref);
        if (id == null) id = synthId(ref);
        // Falls schon persistent -> Fokus
        if (focusPersistentTab(id)) return;
        // Falls Preview denselben Inhalt zeigt -> Promotion
        if (previewEntry != null && !previewEntry.persistent && id.equals(previewId)) {
            promotePreviewToPersistent(id);
            return;
        }
        createPersistentTab(id, ref);
    }

    public boolean isPersistentForModel(Object modelRef) {
        String id = extractId(modelRef);
        return id != null && persistentTabs.containsKey(id);
    }

    /**
     * Liefert eine unveränderliche Liste aller Model-Referenzen, die aktuell in persistenten Tabs geöffnet sind.
     * Vorbereitung für späteres Speichern des UI-States (Issue #25).
     */
    public java.util.List<Object> listPersistentModelRefs() {
        java.util.List<Object> out = new java.util.ArrayList<>();
        for (TabEntry e : persistentTabs.values()) if (e.modelRef != null) out.add(e.modelRef);
        return java.util.Collections.unmodifiableList(out);
    }

    /** Liefert Index des Preview-Tabs oder -1. */
    public int getPreviewTabIndex() {
        if (previewEntry == null) return -1;
        return editorTabs.indexOfComponent(previewEntry.component);
    }

    /** Ist Komponente der Preview-Tab? */
    public boolean isPreviewComponent(Component c) {
        return previewEntry != null && previewEntry.component == c;
    }

    // Optionale explizite Promotion via externer Änderung
    public void notifyContentModified(Object modelRef) {
        if (previewEntry == null || previewEntry.persistent) return;
        String id = extractId(modelRef);
        if (id != null && id.equals(previewId)) {
            promotePreviewToPersistent(id);
        }
    }

    // ========================= Interne Hilfen =========================

    private boolean focusPersistentTab(String id) {
        TabEntry e = persistentTabs.get(id);
        if (e == null) return false;
        int idx = editorTabs.indexOfComponent(e.component);
        if (idx >= 0) {
            editorTabs.setSelectedIndex(idx);
            return true;
        }
        // verwaist -> entfernen
        persistentTabs.remove(id);
        return false;
    }

    private void createOrUpdatePreview(String id, Object ref) {
        Component panel = buildEditorPanelFor(ref);
        if (panel == null) return;
        String title = derivePreviewTitle(ref);
        if (previewEntry == null || editorTabs.indexOfComponent(previewEntry.component) < 0) {
            previewEntry = new TabEntry();
            previewEntry.component = panel;
            previewEntry.modelRef = ref;
            previewEntry.id = id;
            previewEntry.persistent = false;
            previewEntry.title = title;
            previewId = id;
            editorTabs.addTab(title, panel);
            editorTabs.setSelectedIndex(editorTabs.indexOfComponent(panel));
        } else {
            int idx = editorTabs.indexOfComponent(previewEntry.component);
            previewEntry.component = panel;
            previewEntry.modelRef = ref;
            previewEntry.id = id;
            previewEntry.title = title;
            previewId = id;
            editorTabs.setComponentAt(idx, panel);
            editorTabs.setTitleAt(idx, title);
            editorTabs.setSelectedIndex(idx);
        }
    }

    private void promotePreviewToPersistent(String id) {
        if (previewEntry == null) return;
        previewEntry.persistent = true;
        int idx = editorTabs.indexOfComponent(previewEntry.component);
        if (idx >= 0) {
            editorTabs.setTabComponentAt(idx, new ClosableTabHeader(
                    editorTabs,
                    previewEntry.component,
                    previewEntry.title,
                    () -> persistentTabs.remove(id)
            ));
        }
        persistentTabs.put(id, previewEntry);
        previewEntry = null;
        previewId = null;
    }

    private void createPersistentTab(String id, Object ref) {
        Component panel = buildEditorPanelFor(ref);
        if (panel == null) return;
        TabEntry e = new TabEntry();
        e.component = panel;
        e.modelRef = ref;
        e.id = id;
        e.persistent = true;
        e.title = derivePersistentTitle(ref);
        persistentTabs.put(id, e);
        editorTabs.addTab(e.title, e.component);
        int idx = editorTabs.indexOfComponent(e.component);
        editorTabs.setTabComponentAt(idx, new ClosableTabHeader(
                editorTabs,
                e.component,
                e.title,
                () -> persistentTabs.remove(id)
        ));
        editorTabs.setSelectedIndex(idx);
    }

    // Entfernte automatische Promotion-Mechanik
    // promotePreviewIfModified / attachAutoPromoteListener entfallen

    private Component buildEditorPanelFor(Object ref) {
        if (ref instanceof RootNode) return new RootScopeEditorTab((RootNode) ref);
        if (ref instanceof TestSuite) return new SuiteScopeEditorTab((TestSuite) ref);
        if (ref instanceof TestCase) return new CaseScopeEditorTab((TestCase) ref);
        if (ref instanceof TestAction) return new de.bund.zrb.ui.tabs.ActionEditorTab((TestAction) ref);
        JOptionPane.showMessageDialog(parent, "Kein Editor für Typ: " + ref.getClass().getSimpleName());
        return null;
    }

    private String derivePreviewTitle(Object ref) { return derivePersistentTitle(ref); }

    private String derivePersistentTitle(Object ref) {
        if (ref instanceof RootNode) return "Root Scope";
        if (ref instanceof TestSuite) return "Suite: " + safe(((TestSuite) ref).getName());
        if (ref instanceof TestCase) return "Case: " + safe(((TestCase) ref).getName());
        if (ref instanceof TestAction) return "Action: " + safe(((TestAction) ref).getAction());
        return "Editor";
    }

    private static String safe(String s) { return (s == null || s.trim().isEmpty()) ? "(unnamed)" : s.trim(); }

    private String extractId(Object ref) {
        if (ref == null) return null;
        try {
            java.lang.reflect.Method m = ref.getClass().getMethod("getId");
            Object v = m.invoke(ref); // korrekte Reflection-Nutzung
            if (v == null) return null;
            String s = String.valueOf(v).trim();
            return s.isEmpty() ? null : s;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String synthId(Object ref) {
        return ref.getClass().getName() + "@" + System.identityHashCode(ref);
    }
}
