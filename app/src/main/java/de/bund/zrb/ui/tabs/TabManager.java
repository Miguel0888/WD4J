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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TabManager verwaltet genau EIN Preview-Tab (flüchtig) und beliebig viele persistente Tabs.
 * Persistente Tabs bekommen einen ClosableTabHeader. Preview-Tab nicht.
 * Unterscheidung erfolgt NICHT über einen Titelpräfix, sondern über interne Metadaten.
 */
public class TabManager implements NodeOpenHandler {

    /** Interne Repräsentation eines Tabs. */
    private static class TabEntry {
        Component component;
        Object modelRef; // kann RootNode/TestSuite/TestCase/TestAction sein
        boolean persistent; // false = Preview
        String title; // angezeigter Titel (ohne "Preview:"-Präfix)
    }

    private final JTabbedPane editorTabs;
    private final Component parent;
    private final List<TabEntry> entries = new ArrayList<>();
    private TabEntry previewEntry; // explizite Referenz statt Suche

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

        if (focusExistingPersistentTabIfPresent(ref)) {
            return; // persistenter Tab vorhanden -> kein Preview-Update nötig
        }

        // Preview neu oder ersetzen
        createOrReplacePreview(ref);
    }

    /** Öffnet einen persistenten Tab oder fokussiert existierenden. */
    @Override
    public void openInNewTab(TestNode node) {
        if (node == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        if (focusExistingPersistentTabIfPresent(ref)) {
            return; // schon da
        }
        // Falls Preview den gleichen Ref zeigt -> Promotion
        if (previewEntry != null && Objects.equals(previewEntry.modelRef, ref) && !previewEntry.persistent) {
            promotePreview(previewEntry);
            return;
        }
        createPersistentTab(ref);
    }

    /** Prüft ob für modelRef bereits persistenter Tab existiert und fokussiert ihn. */
    public boolean focusExistingPersistentTabIfPresent(Object modelRef) {
        for (int i = 0; i < entries.size(); i++) {
            TabEntry e = entries.get(i);
            if (e.persistent && Objects.equals(e.modelRef, modelRef)) {
                int idx = editorTabs.indexOfComponent(e.component);
                if (idx >= 0) {
                    editorTabs.setSelectedIndex(idx);
                    return true;
                } else {
                    entries.remove(i);
                    i--;
                }
            }
        }
        return false;
    }

    /** Externe Benachrichtigung: Inhalt im Preview wurde wirklich geändert -> Promotion durchführen. */
    public void notifyContentModified(Object modelRef) {
        if (previewEntry != null && !previewEntry.persistent && Objects.equals(previewEntry.modelRef, modelRef)) {
            promotePreview(previewEntry);
        }
    }

    /** Liefert Index des Preview-Tabs oder -1. */
    public int getPreviewTabIndex() {
        TabEntry preview = getPreviewEntry();
        if (preview == null) return -1;
        return editorTabs.indexOfComponent(preview.component);
    }

    /** Ist Komponente der Preview-Tab? */
    public boolean isPreviewComponent(Component c) {
        TabEntry preview = getPreviewEntry();
        return preview != null && preview.component == c;
    }

    /**
     * Liefert eine unveränderliche Liste aller Model-Referenzen, die aktuell in persistenten Tabs geöffnet sind.
     * Vorbereitung für späteres Speichern des UI-States (Issue #25).
     */
    public java.util.List<Object> listPersistentModelRefs() {
        java.util.List<Object> out = new java.util.ArrayList<>();
        for (TabEntry e : entries) {
            if (e.persistent && e.modelRef != null) out.add(e.modelRef);
        }
        return java.util.Collections.unmodifiableList(out);
    }

    /** Prüft ob für das gegebene Modell bereits ein persistenter Tab existiert. */
    public boolean isPersistentForModel(Object modelRef) {
        if (modelRef == null) return false;
        for (TabEntry e : entries) {
            if (e.persistent && java.util.Objects.equals(e.modelRef, modelRef)) return true;
        }
        return false;
    }

    // ========================= Interne Hilfen =========================

    private TabEntry getPreviewEntry() { return previewEntry; }

    private void promotePreview(TabEntry preview) {
        preview.persistent = true;
        int idx = editorTabs.indexOfComponent(preview.component);
        if (idx >= 0) {
            // Closable Header mit onCloseHook setzen
            editorTabs.setTabComponentAt(idx, new ClosableTabHeader(
                    editorTabs,
                    preview.component,
                    preview.title,
                    () -> {
                        entries.remove(preview);
                        if (previewEntry == preview) previewEntry = null;
                    }
            ));
        }
        // Preview-Eintrag loslösen
        if (previewEntry == preview) previewEntry = null;
    }

    private void createOrReplacePreview(Object ref) {
        Component panel = buildEditorPanelFor(ref);
        if (panel == null) return;
        String title = derivePreviewTitle(ref);

        if (previewEntry == null || editorTabs.indexOfComponent(previewEntry.component) < 0) {
            previewEntry = new TabEntry();
            previewEntry.component = panel;
            previewEntry.modelRef = ref;
            previewEntry.persistent = false;
            previewEntry.title = title;
            entries.add(previewEntry);
            editorTabs.addTab(previewEntry.title, previewEntry.component);
            editorTabs.setSelectedIndex(editorTabs.indexOfComponent(previewEntry.component));
        } else {
            int idx = editorTabs.indexOfComponent(previewEntry.component);
            previewEntry.component = panel; // neue Komponente setzen
            previewEntry.modelRef = ref;
            previewEntry.title = title;
            editorTabs.setComponentAt(idx, panel);
            editorTabs.setTitleAt(idx, title);
            editorTabs.setSelectedIndex(idx);
        }
    }

    private void createPersistentTab(Object ref) {
        Component panel = buildEditorPanelFor(ref);
        if (panel == null) return;
        TabEntry entry = new TabEntry();
        entry.component = panel;
        entry.modelRef = ref;
        entry.persistent = true;
        entry.title = derivePersistentTitle(ref);
        entries.add(entry);
        editorTabs.addTab(entry.title, entry.component);
        int idx = editorTabs.indexOfComponent(entry.component);
        editorTabs.setTabComponentAt(idx, new ClosableTabHeader(
                editorTabs,
                entry.component,
                entry.title,
                () -> entries.remove(entry)
        ));
        editorTabs.setSelectedIndex(idx);
    }

    // ========================= Panel-Fabriken & Titel =========================

    private Component buildEditorPanelFor(Object ref) {
        if (ref instanceof RootNode) return new RootScopeEditorTab((RootNode) ref);
        if (ref instanceof TestSuite) return new SuiteScopeEditorTab((TestSuite) ref);
        if (ref instanceof TestCase) return new CaseScopeEditorTab((TestCase) ref);
        if (ref instanceof TestAction) return new de.bund.zrb.ui.tabs.ActionEditorTab((TestAction) ref);
        JOptionPane.showMessageDialog(parent, "Kein Editor für Typ: " + ref.getClass().getSimpleName());
        return null;
    }

    private String derivePreviewTitle(Object ref) {
        // Gleiche Titel wie persistent, nur intern als Preview markiert
        return derivePersistentTitle(ref);
    }

    private String derivePersistentTitle(Object ref) {
        if (ref instanceof RootNode) return "Root Scope";
        if (ref instanceof TestSuite) return "Suite: " + safe(((TestSuite) ref).getName());
        if (ref instanceof TestCase) return "Case: " + safe(((TestCase) ref).getName());
        if (ref instanceof TestAction) return "Action: " + safe(((TestAction) ref).getAction());
        return "Editor";
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "(unnamed)" : s.trim();
    }
}
