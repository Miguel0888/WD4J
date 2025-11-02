package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.*;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.giveneditor.CaseScopeEditorTab;
import de.bund.zrb.ui.giveneditor.RootScopeEditorTab;
import de.bund.zrb.ui.giveneditor.SuiteScopeEditorTab;
import de.bund.zrb.ui.tabs.ActionEditorTab;
import de.bund.zrb.ui.tabs.ClosableTabHeader;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Öffnet Editor-Tabs in der zentralen Mitte (editorTabs).
 *
 * - Root / Suite / Case Tabs werden per Titel reused.
 * - Action Tabs werden NIE reused (immer neuer Tab mit #Counter).
 */
public class EditorTabOpener {

    private static int actionCounter = 1;

    public static void openEditorTab(Component parent,
                                     JTabbedPane editorTabs,
                                     TestNode node) {

        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        // Root
        if (ref instanceof RootNode) {
            RootNode rn = (RootNode) ref;
            String tabTitle = "Root Scope";
            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }
            RootScopeEditorTab panel = new RootScopeEditorTab(rn);
            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // Suite
        if (ref instanceof TestSuite) {
            TestSuite suite = (TestSuite) ref;
            String tabTitle = "Suite: " + safe(suite.getName());
            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }
            SuiteScopeEditorTab panel = new SuiteScopeEditorTab(suite);
            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // Case
        if (ref instanceof TestCase) {
            TestCase testCase = (TestCase) ref;
            String tabTitle = "Case: " + safe(testCase.getName());
            int idx = findTabByTitle(editorTabs, tabTitle);
            if (idx >= 0) {
                editorTabs.setSelectedIndex(idx);
                return;
            }
            CaseScopeEditorTab panel = new CaseScopeEditorTab(testCase);
            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // Action
        if (ref instanceof TestAction) {
            TestAction action = (TestAction) ref;

            // Givens/Sichtbarkeit zusammentragen
            List<GivenCondition> givensForThisAction = collectRelevantGivens(action);

            // Echten ActionEditorTab aufmachen
            ActionEditorTab panel = new ActionEditorTab(action, givensForThisAction);

            String tabTitle = "Action: " + safe(action.getAction()) + " (#" + (actionCounter++) + ")";
            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // Fallback
        JOptionPane.showMessageDialog(
                parent,
                "Kein Editor für Typ: " + ref.getClass().getSimpleName()
        );
    }

    private static int findTabByTitle(JTabbedPane tabs, String wanted) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (wanted.equals(tabs.getTitleAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "(unnamed)" : s.trim();
    }

    /**
     * Sammle alle relevanten Givens für die Action.
     *
     * Fachlogik jetzt KORREKT:
     *  - Case:
     *      - case.getGiven()
     *      - case.getTemplates() (falls vorhanden)
     *    (KEIN beforeEach auf Case-Ebene!)
     *
     *  - Suite:
     *      - suite.getBeforeEach()
     *      - suite.getTemplates()
     *    (suite.getBeforeAll() wird NICHT für Auswahl benutzt)
     *
     *  - Root:
     *      - root.getBeforeEach()
     *      - root.getTemplates()
     *    (root.getBeforeAll() NICHT)
     */
    private static List<GivenCondition> collectRelevantGivens(TestAction action) {
        List<GivenCondition> result = new ArrayList<GivenCondition>();
        if (action == null) return result;

        TestRegistry repo = TestRegistry.getInstance();

        // von der Action hoch zum Case
        TestCase tc = repo.findCaseById(action.getParentId());
        if (tc != null) {
            // Case-eigene Givens
            safeAddAll(result, tc.getGiven());

            // Case-Templates (falls vorhanden)
            safeAddAll(result, callGetTemplatesIfExists(tc));

            // dann Suite
            TestSuite suite = repo.findSuiteById(tc.getParentId());
            if (suite != null) {
                safeAddAll(result, suite.getBeforeEach());
                safeAddAll(result, suite.getTemplates());
            }
        }

        // Root immer noch oben drauf
        RootNode root = repo.getRoot();
        if (root != null) {
            safeAddAll(result, root.getBeforeEach());
            safeAddAll(result, root.getTemplates());
        }

        return result;
    }

    private static void safeAddAll(List<GivenCondition> out, List<GivenCondition> in) {
        if (in == null) return;
        out.addAll(in);
    }

    /**
     * Case.getTemplates() ist evtl. vorhanden, evtl. nicht.
     * Wir greifen defensiv per Reflection zu, ohne den Case zu zwingen ein Interface zu ändern.
     */
    @SuppressWarnings("unchecked")
    private static List<GivenCondition> callGetTemplatesIfExists(TestCase tc) {
        if (tc == null) return null;
        try {
            java.lang.reflect.Method m = tc.getClass().getMethod("getTemplates");
            Object v = m.invoke(tc);
            if (v instanceof List<?>) {
                return (List<GivenCondition>) v;
            }
        } catch (Exception ignore) {
            // Case hat (noch) keine Templates -> null zurück
        }
        return null;
    }
}
