package de.bund.zrb.service;

import de.bund.zrb.model.RootNode;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import de.bund.zrb.runtime.RuntimeVariableContext;
import de.bund.zrb.runtime.TestRunContext;
import de.bund.zrb.runtime.ValueScope;
import de.bund.zrb.ui.TestPlayerUi;
import de.bund.zrb.ui.components.log.StepLog;
import de.bund.zrb.ui.components.log.TestExecutionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestRunnerCoreTest {

    private BrowserServiceImpl browserService;
    private GivenConditionExecutor givenExecutor;
    private TestPlayerUi drawerRef;
    private TestExecutionLogger logger;

    private TestRunner runner;
    private TestRunContext runContext;

    @BeforeEach
    void setUp() throws Exception {
        browserService = mock(BrowserServiceImpl.class);
        givenExecutor = mock(GivenConditionExecutor.class);
        drawerRef = mock(TestPlayerUi.class);
        logger = mock(TestExecutionLogger.class);

        runner = new TestRunner(browserService, givenExecutor, drawerRef, logger);

        // Inject fresh TestRunContext
        TestRunContext ctx = new TestRunContext(ExpressionRegistryImpl.getInstance());
        Field f = TestRunner.class.getDeclaredField("runContext");
        f.setAccessible(true);
        f.set(runner, ctx);
        this.runContext = ctx;

        // Clear maps in RuntimeVariableContext to ensure isolation
        RuntimeVariableContext vars = runContext.getVars();
        Field rv = RuntimeVariableContext.class.getDeclaredField("rootVars"); rv.setAccessible(true); ((java.util.Map) rv.get(vars)).clear();
        Field sv = RuntimeVariableContext.class.getDeclaredField("suiteVars"); sv.setAccessible(true); ((java.util.Map) sv.get(vars)).clear();
        Field cv = RuntimeVariableContext.class.getDeclaredField("caseVars"); cv.setAccessible(true); ((java.util.Map) cv.get(vars)).clear();

        // Reset Root in TestRegistry
        Field rootField = TestRegistry.class.getDeclaredField("root");
        rootField.setAccessible(true);
        rootField.set(TestRegistry.getInstance(), new RootNode());
    }

    // -------------------------------------------------------------------------
    // User-Resolution Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resolveEffectiveUserForAction: use user from action when set")
    void resolveEffectiveUser_usesActionUserWhenPresent() {
        runContext.getVars().enterCase();
        TestAction action = new TestAction();
        action.setUser("  alice  ");
        String effectiveUser = invokeResolveEffectiveUser(runContext, action);
        assertEquals("alice", effectiveUser);
    }

    @Test
    @DisplayName("resolveEffectiveUserForAction: use ctx.user when action user is empty")
    void resolveEffectiveUser_usesContextUserWhenActionUserEmpty() {
        runContext.getVars().enterCase();
        TestAction action = new TestAction();
        action.setUser(null);
        runContext.getVars().setCaseVar("user", " bob ");
        String effectiveUser = invokeResolveEffectiveUser(runContext, action);
        assertEquals("bob", effectiveUser);
    }

    @Test
    @DisplayName("resolveEffectiveUserForAction: throw when no user in action or context")
    void resolveEffectiveUser_throwsWhenNoUserAvailable() throws Exception {
        runContext.getVars().enterCase();
        TestAction action = new TestAction();
        action.setUser("");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invokeResolveEffectiveUser(runContext, action));
        assertTrue(ex.getMessage().contains("Tool invocation failed: user darf nicht leer sein."));
    }

    /**
     * Helper to call private resolveEffectiveUserForAction but unwrap InvocationTargetException,
     * so that assertThrows can see the real IllegalStateException.
     */
    private String invokeResolveEffectiveUser(TestRunContext ctx, TestAction action) {
        try {
            Method m = TestRunner.class.getDeclaredMethod(
                    "resolveEffectiveUserForAction",
                    TestRunContext.class,
                    TestAction.class
            );
            m.setAccessible(true);
            return (String) m.invoke(runner, ctx, action);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Init-Reihenfolge Root: Templates -> BeforeAll -> BeforeEach
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Root: BeforeAll runs before BeforeEach so that BeforeEach can use {{user}}")
    void rootBeforeAllRunsBeforeBeforeEach() throws Exception {
        RootNode root = new RootNode();
        // Templates
        root.getTemplates().put("otpTemplate", "OTP-{{user}}");
        root.getTemplatesEnabled().put("otpTemplate", Boolean.TRUE);
        // BeforeAll
        root.getBeforeAll().put("user", "alice");
        root.getBeforeAllEnabled().put("user", Boolean.TRUE);
        // BeforeEach
        root.getBeforeEach().put("home", "HOME-{{user}}");
        root.getBeforeEachEnabled().put("home", Boolean.TRUE);

        // Replace Root in TestRegistry via reflection
        Field rf = TestRegistry.class.getDeclaredField("root");
        rf.setAccessible(true);
        rf.set(TestRegistry.getInstance(), root);

        TestSuite suite = new TestSuite();
        suite.setId("suite-1");
        // Suite stays empty for this test

        Method initSuite = TestRunner.class.getDeclaredMethod("initSuiteSymbols", TestSuite.class);
        initSuite.setAccessible(true);
        initSuite.invoke(runner, suite);

        TestCase testCase = new TestCase();
        testCase.setId("case-1");
        // Case stays empty

        Method initCase = TestRunner.class.getDeclaredMethod(
                "initCaseSymbols",
                de.bund.zrb.ui.TestNode.class,
                TestCase.class
        );
        initCase.setAccessible(true);
        initCase.invoke(runner, new Object[]{null, testCase});

        ValueScope scope = runContext.getVars().buildCaseScope();
        assertEquals("alice", scope.lookupVar("user"));
        assertEquals("HOME-alice", scope.lookupVar("home"));
    }

    // -------------------------------------------------------------------------
    // playSingleAction: ensure it internally uses ctx.user when action user is null
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("playSingleAction (internal): uses ctx.user when action user not set")
    void playSingleActionUsesContextUser() {
        runContext.getVars().enterCase();
        runContext.getVars().setCaseVar("user", "alice");

        TestAction action = new TestAction();
        action.setUser(null);

        String effective = invokeResolveEffectiveUser(runContext, action);
        assertEquals("alice", effective);
    }

    // Optional helper to invoke private playSingleAction(TestRunContext, TestAction, StepLog)
    @SuppressWarnings("unused")
    private boolean invokePlaySingleAction(TestRunContext ctx, TestAction action, StepLog log) {
        try {
            Method m = TestRunner.class.getDeclaredMethod(
                    "playSingleAction",
                    TestRunContext.class,
                    TestAction.class,
                    StepLog.class
            );
            m.setAccessible(true);
            return (Boolean) m.invoke(runner, ctx, action, log);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
