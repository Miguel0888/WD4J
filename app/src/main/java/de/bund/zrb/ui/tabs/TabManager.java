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

/**
 * Verwaltet Editor-Tabs in der Mitte: Preview-Tab (einzigartig) und persistente Tabs.
 *
 * - Preview-Tab: wird bei Node-Auswahl überschrieben. Titel "Preview" mit dynamischem Suffix.
 * - Persistente Tabs: werden bewusst vom Nutzer geöffnet und bleiben bestehen.
 *
 * Diese Klasse kapselt die Öffnungslogik, so dass Listener nur noch klar benannte Methoden aufrufen.
 */
public class TabManager implements NodeOpenHandler {

    private final JTabbedPane editorTabs;
    private final Component parent;

    private static final String PREVIEW_TITLE_PREFIX = "Preview: ";

    public TabManager(Component parent, JTabbedPane editorTabs) {
        this.parent = parent;
        this.editorTabs = editorTabs;
    }

    // ========================= Preview-API =========================

    /**
     * Zeigt den Inhalt eines LeftDrawer-Nodes im Preview-Tab (wird bei neuer Auswahl ersetzt).
     */
    public void showInPreview(TestNode node) {
        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        String title = PREVIEW_TITLE_PREFIX + deriveTitleSuffix(ref);
        Component panel = buildEditorPanelFor(ref);
        if (panel == null) return;

        int previewIdx = findPreviewTabIndex();
        if (previewIdx >= 0) {
            editorTabs.setComponentAt(previewIdx, panel);
            editorTabs.setTitleAt(previewIdx, title);
            editorTabs.setSelectedIndex(previewIdx);
        } else {
            editorTabs.addTab(title, panel);
            int idx = editorTabs.indexOfComponent(panel);
            editorTabs.setSelectedIndex(idx);
        }
    }

    // ========================= Persistente Tabs =========================

    @Override
    public void openInNewTab(TestNode node) {
        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        Component panel = buildEditorPanelFor(ref);
        if (panel == null) return;

        String title = derivePersistentTitle(ref);
        editorTabs.addTab(title, panel);
        int newIdx = editorTabs.indexOfComponent(panel);
        // ClosableTabHeader nur für persistente Tabs, Preview bleibt ohne Close-X
        editorTabs.setTabComponentAt(newIdx, new de.bund.zrb.ui.tabs.ClosableTabHeader(editorTabs, panel, title));
        editorTabs.setSelectedIndex(newIdx);
    }

    // ========================= Fabriken / Helpers =========================

    private Component buildEditorPanelFor(Object ref) {
        if (ref instanceof RootNode) {
            return new RootScopeEditorTab((RootNode) ref);
        }
        if (ref instanceof TestSuite) {
            return new SuiteScopeEditorTab((TestSuite) ref);
        }
        if (ref instanceof TestCase) {
            return new CaseScopeEditorTab((TestCase) ref);
        }
        if (ref instanceof TestAction) {
            return new de.bund.zrb.ui.tabs.ActionEditorTab((TestAction) ref);
        }
        JOptionPane.showMessageDialog(parent, "Kein Editor für Typ: " + ref.getClass().getSimpleName());
        return null;
    }

    private String deriveTitleSuffix(Object ref) {
        if (ref instanceof RootNode) return "Root Scope";
        if (ref instanceof TestSuite) return "Suite: " + safe(((TestSuite) ref).getName());
        if (ref instanceof TestCase) return "Case: " + safe(((TestCase) ref).getName());
        if (ref instanceof TestAction) {
            TestAction a = (TestAction) ref;
            String base = safe(a.getAction());
            if (a.getValue() != null && !a.getValue().isEmpty()) return base + " [" + a.getValue() + "]";
            if (a.getSelectedSelector() != null && !a.getSelectedSelector().isEmpty()) return base + " [" + a.getSelectedSelector() + "]";
            return base;
        }
        return "Unbekannt";
    }

    private String derivePersistentTitle(Object ref) {
        // Für persistente Tabs nutzen wir die bisherigen Titel-Konventionen, ohne Zählerzwang.
        if (ref instanceof RootNode) return "Root Scope";
        if (ref instanceof TestSuite) return "Suite: " + safe(((TestSuite) ref).getName());
        if (ref instanceof TestCase) return "Case: " + safe(((TestCase) ref).getName());
        if (ref instanceof TestAction) return "Action: " + safe(((TestAction) ref).getAction());
        return "Editor";
    }

    private int findPreviewTabIndex() {
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            String t = editorTabs.getTitleAt(i);
            if (t != null && t.startsWith(PREVIEW_TITLE_PREFIX)) {
                return i;
            }
        }
        return -1;
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "(unnamed)" : s.trim();
    }
}

