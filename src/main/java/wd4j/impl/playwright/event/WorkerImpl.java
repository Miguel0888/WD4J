package wd4j.impl.playwright.event;

import wd4j.api.JSHandle;
import wd4j.api.Worker;
import wd4j.impl.webdriver.event.WDScriptEvent;

import java.util.function.Consumer;

public class WorkerImpl implements Worker {
    public WorkerImpl(WDScriptEvent.RealmCreated realmCreated) {
        // TODO: Implement this
    }

    public WorkerImpl(WDScriptEvent.RealmDestroyed realmDestroyed) {
        // TODO: Implement this
    }

    @Override
    public void onClose(Consumer<Worker> handler) {

    }

    @Override
    public void offClose(Consumer<Worker> handler) {

    }

    @Override
    public Object evaluate(String expression, Object arg) {
        return null;
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        return null;
    }

    @Override
    public String url() {
        return "";
    }

    @Override
    public Worker waitForClose(WaitForCloseOptions options, Runnable callback) {
        return null;
    }
}
