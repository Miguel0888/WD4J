package de.bund.zrb.controller;

/**
 * EventHandler speichert eine Lambda für das Registrieren und eine für das Deregistrieren eines Events.
 */
public class EventHandler {
    private final Runnable registerAction;
    private final Runnable deregisterAction;

    public EventHandler(Runnable registerAction, Runnable deregisterAction) {
        this.registerAction = registerAction;
        this.deregisterAction = deregisterAction;
    }

    public void register() {
        registerAction.run();
    }

    public void deregister() {
        deregisterAction.run();
    }
}
