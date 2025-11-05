package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Promote action-level users to case or suite if uniform.
 *
 * Rules:
 * - If all actions in a case share the same non-empty user -> put into case.before["user"], set action.user = null.
 * - After doing that for all cases, if all cases in the suite share the same non-empty case user ->
 *   put into suite.beforeAll["user"], remove user from all case.before, keep action.user = null.
 *
 * Keep Java 8 and be defensive against null maps.
 */
public final class UserPromotionUtil {

    private UserPromotionUtil() {}

    /** Promote within a single case. Return the promoted user or null. */
    public static String promoteCaseUser(TestCase tc) {
        if (tc == null) return null;
        List<TestAction> when = tc.getWhen();
        if (when == null || when.isEmpty()) return null;

        String u = uniformActionUser(when);
        if (isBlank(u)) return null;

        // write to case.before
        Map<String,String> before = tc.getBefore();
        if (before != null) {
            before.put("user", u);
        }

        // clear user on actions (inherit from case)
        for (int i = 0; i < when.size(); i++) {
            TestAction a = when.get(i);
            if (a != null) a.setUser(null);
        }
        return u;
    }

    /** Promote within a whole suite: first per case, then possibly to suite level. */
    public static void promoteSuiteUsers(TestSuite suite) {
        if (suite == null) return;

        // 1) promote per case
        List<TestCase> cases = suite.getTestCases();
        if (cases == null || cases.isEmpty()) return;

        List<String> caseUsers = new ArrayList<String>(cases.size());
        for (int i = 0; i < cases.size(); i++) {
            TestCase tc = cases.get(i);
            String promoted = promoteCaseUser(tc); // may be null
            caseUsers.add(promoted);
        }

        // 2) if all cases have the same non-empty user -> promote to suite.beforeAll
        String suiteUser = uniform(caseUsers);
        if (isBlank(suiteUser)) return;

        Map<String,String> beforeAll = suite.getBeforeAll();
        if (beforeAll != null) {
            beforeAll.put("user", suiteUser);
        }

        // remove user from each case.before
        for (int i = 0; i < cases.size(); i++) {
            TestCase tc = cases.get(i);
            Map<String,String> before = (tc != null) ? tc.getBefore() : null;
            if (before != null) {
                before.remove("user");
            }
        }
    }

    // ---------- helpers ----------

    private static String uniformActionUser(List<TestAction> actions) {
        String candidate = null;
        for (int i = 0; i < actions.size(); i++) {
            TestAction a = actions.get(i);
            if (a == null) continue;
            String u = trimToNull(a.getUser());
            if (u == null) {
                // if any action has null/empty user => not uniform
                return null;
            }
            if (candidate == null) {
                candidate = u;
            } else if (!candidate.equals(u)) {
                return null;
            }
        }
        return candidate;
    }

    private static String uniform(List<String> values) {
        String cand = null;
        for (int i = 0; i < values.size(); i++) {
            String v = trimToNull(values.get(i));
            if (v == null) return null;
            if (cand == null) cand = v;
            else if (!cand.equals(v)) return null;
        }
        return cand;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().length() == 0; }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() == 0 ? null : t;
    }
}
