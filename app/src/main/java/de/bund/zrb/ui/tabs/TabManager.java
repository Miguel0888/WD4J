package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestActionUpdatedEvent;
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
    private static boolean tmLogEnabled() {
        return Boolean.getBoolean("wd4j.log.tabmanager") || Boolean.getBoolean("wd4j.debug");
    }
    private static void tmLog(String msg) { if (tmLogEnabled()) System.out.println(msg); }
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
        // Kein Preview-Rebuild hier: ActionEditorTab subscribed selbst und aktualisiert nur die Description.
        // Optional: leichte Revalidation, falls benötigt.
        ApplicationEventBus.getInstance().subscribe(TestActionUpdatedEvent.class, ev -> {
            if (previewEntry != null && !previewEntry.persistent && previewEntry.modelRef == ev.getPayload()) {
                int idx = editorTabs.indexOfComponent(previewEntry.component);
                if (idx >= 0) {
                    previewEntry.component.revalidate();
                    previewEntry.component.repaint();
                }
            }
        });
    }

    // ========================= Öffentliche API =========================

    /** Zeige Node im Preview-Tab oder fokussiere bestehenden persistenten Tab. */
    public void showInPreview(TestNode node) {
        if (node == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;
        String id = extractId(ref);
        tmLog("[TabManager] showInPreview -> class=" + ref.getClass().getSimpleName() + " id=" + id);
        if (id != null && focusPersistentTab(id)) {
            tmLog("[TabManager] showInPreview: focusing existing persistent tab id=" + id);
            return;
        }
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
        tmLog("[TabManager] openInNewTab -> class=" + ref.getClass().getSimpleName() + " id=" + id);
        if (focusPersistentTab(id)) {
            tmLog("[TabManager] openInNewTab: tab already persistent, focusing id=" + id);
            return;
        }
        if (previewEntry != null && !previewEntry.persistent && id.equals(previewId)) {
            tmLog("[TabManager] openInNewTab: promoting preview id=" + id);
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
            tmLog("[TabManager] focusPersistentTab: found persistent id=" + id + " idx=" + idx);
            editorTabs.setSelectedIndex(idx);
            return true;
        }
        tmLog("[TabManager] focusPersistentTab: stale entry removed id=" + id);
        persistentTabs.remove(id);
        return false;
    }

    private void createOrUpdatePreview(String id, Object ref) {
        tmLog("[TabManager] createOrUpdatePreview id=" + id + " previewExists=" + (previewEntry != null));
        Component inner = buildEditorPanelFor(ref);
        if (inner == null) return;
        Component panel = wrapIfSaveable(inner);
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
            int idx = editorTabs.indexOfComponent(panel);
            // Sicherstellen: eigener Header mit Pin-Button und ohne Close
            PreviewTabHeader header = new PreviewTabHeader(editorTabs, panel, title, () -> {
                if (previewEntry != null && !previewEntry.persistent) {
                    // Promotion des aktuellen Preview-Tabs zu persistent
                    promotePreviewToPersistent(previewEntry.id);
                }
            });
            editorTabs.setTabComponentAt(idx, header);
            editorTabs.setSelectedIndex(idx);
            tmLog("[TabManager] createOrUpdatePreview: new preview tab idx=" + idx);
        } else {
            int idx = editorTabs.indexOfComponent(previewEntry.component);
            // Auto-Save des bisherigen Preview-Inhalts
            autoSaveIfSupported(previewEntry.component);

            previewEntry.component = panel;
            previewEntry.modelRef = ref;
            previewEntry.id = id;
            previewEntry.title = title;
            previewId = id;
            editorTabs.setComponentAt(idx, panel);
            editorTabs.setTitleAt(idx, title);
            // Header aktualisieren oder neu setzen
            java.awt.Component existingHeader = editorTabs.getTabComponentAt(idx);
            if (existingHeader instanceof PreviewTabHeader) {
                ((PreviewTabHeader) existingHeader).setTitle(title);
            } else {
                PreviewTabHeader header = new PreviewTabHeader(editorTabs, panel, title, () -> {
                    if (previewEntry != null && !previewEntry.persistent) {
                        promotePreviewToPersistent(previewEntry.id);
                    }
                });
                editorTabs.setTabComponentAt(idx, header);
            }
            editorTabs.setSelectedIndex(idx);
            tmLog("[TabManager] createOrUpdatePreview: replaced preview tab idx=" + idx);
        }
    }

    private Component wrapIfSaveable(Component c) {
        if (c instanceof JComponent && c instanceof Saveable && c instanceof Revertable) {
            return new SaveRevertContainer((JComponent) c);
        }
        return c;
    }

    private void autoSaveIfSupported(Component c) {
        try {
            if (c instanceof SaveRevertContainer) {
                SaveRevertContainer cont = (SaveRevertContainer) c;
                cont.getSaveable().saveChanges();
            } else if (c instanceof Saveable) {
                ((Saveable) c).saveChanges();
            }
        } catch (Throwable t) {
            tmLog("[TabManager] autoSaveIfSupported failed: " + t.getMessage());
        }
    }

    private void promotePreviewToPersistent(String id) {
        if (previewEntry == null) return;
        tmLog("[TabManager] promotePreviewToPersistent id=" + id);
        previewEntry.persistent = true;
        int idx = editorTabs.indexOfComponent(previewEntry.component);
        if (idx >= 0) {
            editorTabs.setTabComponentAt(idx, new ClosableTabHeader(
                    editorTabs,
                    previewEntry.component,
                    previewEntry.title,
                    () -> {
                        tmLog("[TabManager] onClose persistent tab id=" + id);
                        persistentTabs.remove(id);
                    }
            ));
        }
        persistentTabs.put(id, previewEntry);
        previewEntry = null;
        previewId = null;
    }

    private void createPersistentTab(String id, Object ref) {
        tmLog("[TabManager] createPersistentTab id=" + id);
        Component inner = buildEditorPanelFor(ref);
        if (inner == null) return;
        Component panel = wrapIfSaveable(inner);
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
                () -> {
                    tmLog("[TabManager] onClose persistent tab id=" + id);
                    persistentTabs.remove(id);
                }
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
