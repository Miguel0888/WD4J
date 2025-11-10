package de.bund.zrb.service;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.components.log.LogComponent;
import de.bund.zrb.ui.components.log.SuiteLog;
import de.bund.zrb.ui.components.log.TestExecutionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integrationstest für executeAfterAssertions:
 * - Ohne ValidatorType wird immer PASS erwartet – auch bei "ungünstigem" Ausdruck ("false")
 * - Mit ValidatorType=equals und nicht passendem Wert wird FAIL erwartet
 */
public class TestPlayerServiceAssertionsIntegrationTest {

    @BeforeEach
    void setupLogger() {
        // Einfachen Logger registrieren, damit executeAfterAssertions Logs schreiben kann
        JEditorPane pane = new JEditorPane();
        TestExecutionLogger logger = new TestExecutionLogger(pane);
        TestPlayerService.getInstance().registerLogger(logger);
    }

    @SuppressWarnings("unchecked")
    private List<LogComponent> invokeExecuteAfterAssertions(TestNode caseNode, TestCase testCase, SuiteLog parent) {
        try {
            Method m = TestPlayerService.class.getDeclaredMethod(
                    "executeAfterAssertions", TestNode.class, TestCase.class, SuiteLog.class);
            m.setAccessible(true);
            return (List<LogComponent>) m.invoke(TestPlayerService.getInstance(), caseNode, testCase, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Kein ValidatorType -> immer PASS (Expression liefert 'false')")
    void testNoValidatorAlwaysPass() {
        // Root / Suite / Case vorbereiten
        TestRegistry reg = TestRegistry.getInstance();
        RootNode root = reg.getRoot();
        root.getTestSuites().clear();

        TestSuite suite = new TestSuite();
        suite.setName("Suite");
        TestCase tc = new TestCase();
        tc.setName("Case");

        // Case After: Ausdruck der eigentlich 'false' liefert
        Map<String,String> after = tc.getAfter();
        after.put("exprFalse", "false");
        tc.getAfterEnabled().put("exprFalse", Boolean.TRUE);
        tc.getAfterValidatorType().put("exprFalse", ""); // kein Validator
        tc.getAfterValidatorValue().put("exprFalse", "");

        suite.getTestCases().add(tc);
        root.getTestSuites().add(suite);

        // Baumknoten bauen
        TestNode suiteNode = new TestNode("Suite", suite);
        TestNode caseNode = new TestNode("Case", tc);
        suiteNode.add(caseNode);

        // Case-Scope für Assertions initialisieren
        TestPlayerService.getInstance()._testInitCaseScope();
        // Aufruf
        List<LogComponent> logs = invokeExecuteAfterAssertions(caseNode, tc, new SuiteLog("PARENT"));

        // Einen EXPECT StepLog mit exprFalse finden
        boolean found = false;
        for (LogComponent comp : logs) {
            if (comp instanceof de.bund.zrb.ui.components.log.StepLog) {
                de.bund.zrb.ui.components.log.StepLog sl = (de.bund.zrb.ui.components.log.StepLog) comp;
                if (sl.getContent().startsWith("Case/exprFalse")) {
                    found = true;
                    assertTrue(sl.isSuccess(), "Ohne ValidatorType darf die Assertion nicht fehlschlagen – raw='false'.");
                }
            }
        }
        assertTrue(found, "Erwarteter Assertion-Log wurde nicht gefunden.");
    }

    @Test
    @DisplayName("Validator equals -> FAIL bei nicht passendem Wert")
    void testValidatorEqualsFail() {
        TestRegistry reg = TestRegistry.getInstance();
        RootNode root = reg.getRoot();
        root.getTestSuites().clear();

        TestSuite suite = new TestSuite();
        suite.setName("Suite");
        TestCase tc = new TestCase();
        tc.setName("Case");

        // Case After: Ausdruck liefert "ACTUAL"; Validator erwartet "EXPECTED"
        tc.getAfter().put("exprEq", "ACTUAL");
        tc.getAfterEnabled().put("exprEq", Boolean.TRUE);
        tc.getAfterValidatorType().put("exprEq", "equals");
        tc.getAfterValidatorValue().put("exprEq", "EXPECTED");

        suite.getTestCases().add(tc);
        root.getTestSuites().add(suite);

        TestNode suiteNode = new TestNode("Suite", suite);
        TestNode caseNode = new TestNode("Case", tc);
        suiteNode.add(caseNode);

        // Case-Scope für Assertions initialisieren
        TestPlayerService.getInstance()._testInitCaseScope();
        List<LogComponent> logs = invokeExecuteAfterAssertions(caseNode, tc, new SuiteLog("PARENT"));

        boolean found = false;
        for (LogComponent comp : logs) {
            if (comp instanceof de.bund.zrb.ui.components.log.StepLog) {
                de.bund.zrb.ui.components.log.StepLog sl = (de.bund.zrb.ui.components.log.StepLog) comp;
                if (sl.getContent().startsWith("Case/exprEq")) {
                    found = true;
                    assertFalse(sl.isSuccess(), "Mit ValidatorType=equals und ungleichem Wert muss FAIL kommen.");
                }
            }
        }
        assertTrue(found, "Erwarteter Assertion-Log wurde nicht gefunden.");
    }
}
