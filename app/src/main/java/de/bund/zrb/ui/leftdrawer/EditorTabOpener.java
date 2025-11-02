package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.*;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.giveneditor.CaseScopeEditorTab;
import de.bund.zrb.ui.giveneditor.RootScopeEditorTab;
import de.bund.zrb.ui.giveneditor.SuiteScopeEditorTab;
import de.bund.zrb.ui.tabs.ActionEditorTab;
import de.bund.zrb.ui.tabs.ClosableTabHeader;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Öffnet Editor-Tabs in der zentralen Mitte (editorTabs).
 *
 * Verhalten:
 *  - Root / Suite / Case:
 *      Falls Tab mit gleichem Titel schon offen ist -> aktiviere ihn.
 *      Sonst neu öffnen.
 *
 *  - Action:
 *      IMMER ein neuer Tab.
 *      Titel "Action: <actionName> (#<laufendeNummer>)", damit jeder eindeutig ist.
 *
 *  Wichtig: Für Action verwenden wir den echten ActionEditorTab.
 */
public class EditorTabOpener {

    // nur für laufende durchnummerierung der Action-Tabs
    private static int actionCounter = 1;

    public static void openEditorTab(Component parent,
                                     JTabbedPane editorTabs,
                                     TestNode node) {

        if (node == null || editorTabs == null) return;
        Object ref = node.getModelRef();
        if (ref == null) return;

        // ---- RootNode Tab ----
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

        // ---- TestSuite Tab ----
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

        // ---- TestCase Tab ----
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

        // ---- TestAction Tab ----
        if (ref instanceof TestAction) {
            TestAction action = (TestAction) ref;

            // 1. Givens für diese Action sammeln
            List<GivenCondition> givensForThisAction = collectRelevantGivens(action);

            // 2. Deinen bestehenden ActionEditorTab verwenden
            ActionEditorTab panel = new ActionEditorTab(action, givensForThisAction);

            // eindeutiger Titel (KEIN reuse!)
            String tabTitle = "Action: " + safe(action.getAction()) + " (#" + (actionCounter++) + ")";

            editorTabs.addTab(tabTitle, panel);
            int newIdx = editorTabs.indexOfComponent(panel);
            editorTabs.setTabComponentAt(newIdx,
                    new ClosableTabHeader(editorTabs, panel, tabTitle));
            editorTabs.setSelectedIndex(newIdx);
            return;
        }

        // ---- Fallback ----
        JOptionPane.showMessageDialog(
                parent,
                "Kein Editor für Typ: " + ref.getClass().getSimpleName()
        );
    }

    /**
     * Sucht den ersten Tab, dessen Title genau "wanted" entspricht.
     * Für Root / Suite / Case nutzen wir das weiter wie gehabt.
     */
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
     * Ausgehend von einer Action holen wir uns die Givens,
     * die dein alter ActionEditorTab erwartet (givensForThisAction).
     *
     * Wir sammeln:
     *  - alle beforeEach-Variablen hoch bis Suite/Root
     *  - plus Templates (Case/Suite/Root)
     *  - plus alles was der konkrete Case als Given / BeforeEach / Templates hat
     *
     * -> Das gleicht der späteren Sichtbarkeit im Lauf.
     *
     * Für jetzt: wir bauen das mit Hilfe der Registry und parentId-Kette.
     */
    private static List<GivenCondition> collectRelevantGivens(TestAction action) {
        List<GivenCondition> result = new ArrayList<GivenCondition>();
        if (action == null) return result;

        TestRegistry repo = TestRegistry.getInstance();

        // Wir gehen vom Action-ParentCase hoch
        TestCase tc = repo.findCaseById(action.getParentId());
        if (tc != null) {
            // Case-spezifische Givens:
            safeAddAll(result, tc.getGiven());
            safeAddAll(result, tc.getBeforeEach());
            safeAddAll(result, tc.getTemplates());

            // Suite
            TestSuite suite = repo.findSuiteById(tc.getParentId());
            if (suite != null) {
                safeAddAll(result, suite.getBeforeEach());
                safeAddAll(result, suite.getTemplates());
                // suite.getBeforeAll() lassen wir ebenfalls drin,
                // denn dein alter EditorTab kennt aktuell noch keine Unterscheidung
                safeAddAll(result, suite.getBeforeAll());

                // Root
                RootNode root = repo.getRoot();
                if (root != null) {
                    safeAddAll(result, root.getBeforeEach());
                    safeAddAll(result, root.getTemplates());
                    safeAddAll(result, root.getBeforeAll());
                }
            } else {
                // Kein Suite gefunden -> trotzdem Root berücksichtigen
                RootNode root = repo.getRoot();
                if (root != null) {
                    safeAddAll(result, root.getBeforeEach());
                    safeAddAll(result, root.getTemplates());
                    safeAddAll(result, root.getBeforeAll());
                }
            }
        } else {
            // Fallback: keine Case-ID -> nur Root
            RootNode root = repo.getRoot();
            if (root != null) {
                safeAddAll(result, root.getBeforeEach());
                safeAddAll(result, root.getTemplates());
                safeAddAll(result, root.getBeforeAll());
            }
        }

        return result;
    }

    private static void safeAddAll(List<GivenCondition> out, List<GivenCondition> in) {
        if (in == null) return;
        out.addAll(in);
    }
}
