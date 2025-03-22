package wd4j.impl.playwright.event;

import wd4j.api.JSHandle;
import wd4j.api.Worker;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.playwright.JSHandleImpl;
import wd4j.impl.webdriver.event.WDScriptEvent;
import wd4j.impl.webdriver.type.script.WDEvaluateResult;
import wd4j.impl.webdriver.type.script.WDRealm;
import wd4j.impl.webdriver.type.script.WDTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WorkerImpl implements Worker {

    private final String realmId;
    private final WDScriptManager scriptManager;
    private final List<Consumer<Worker>> closeHandlers = new ArrayList<>();
    private boolean isClosed = false;

    public WorkerImpl(WDScriptEvent.RealmCreated realmCreated) {
        this.scriptManager = null; // ToDo: Implement this, how to get the manager? Might be a constructor parameter?
        if (realmCreated == null || realmCreated.getParams() == null) {
            throw new IllegalArgumentException("RealmCreated event must not be null.");
        }
        this.realmId = realmCreated.getParams().getRealm().value();
    }

    public WorkerImpl(WDScriptEvent.RealmDestroyed realmDestroyed) {
        this.scriptManager = null; // ToDo: Implement this, how to get the manager? Might be a constructor parameter?
        if (realmDestroyed == null || realmDestroyed.getParams() == null) {
            throw new IllegalArgumentException("RealmDestroyed event must not be null.");
        }
        this.realmId = realmDestroyed.getParams().getRealm();
        this.isClosed = true;
        notifyCloseHandlers();
    }

    private void notifyCloseHandlers() {
        for (Consumer<Worker> handler : closeHandlers) {
            handler.accept(this);
        }
        closeHandlers.clear();
    }

    @Override
    public void onClose(Consumer<Worker> handler) {
        if (handler == null) return;
        if (isClosed) {
            handler.accept(this);
        } else {
            closeHandlers.add(handler);
        }
    }

    @Override
    public void offClose(Consumer<Worker> handler) {
        closeHandlers.remove(handler);
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        if (isClosed) {
            throw new IllegalStateException("Cannot evaluate on a closed worker.");
        }
        if (scriptManager == null) {
            throw new IllegalStateException("ScriptManager is not available.");
        }

        return scriptManager.evaluate(expression, new WDTarget.RealmTarget(new WDRealm(realmId)), true);
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        // ToDo: Implement this
//        if (isClosed) {
//            throw new IllegalStateException("Cannot evaluate on a closed worker.");
//        }
//        if (scriptManager == null) {
//            throw new IllegalStateException("ScriptManager is not available.");
//        }
//
//        // TODO: Wenn `JSHandle` unterstützt wird, dann entsprechend mappen
//        WDEvaluateResult evaluate = scriptManager.evaluate(expression, new WDTarget.RealmTarget(new WDRealm(realmId)), true);
//        if(evaluate instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
//            return new JSHandleImpl(((WDEvaluateResult.WDEvaluateResultSuccess) evaluate).getResult().getHandle(), new WDRealm(realmId));
//        }
        return null;
    }

    @Override
    public String url() {
        return realmId != null ? realmId : "";
    }

    @Override
    public Worker waitForClose(WaitForCloseOptions options, Runnable callback) {
        if (isClosed) {
            callback.run();
            return this;
        }
        onClose(worker -> callback.run());
        return this;
    }
}
