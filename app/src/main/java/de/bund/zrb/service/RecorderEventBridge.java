package de.bund.zrb.service;

import de.bund.zrb.event.ApplicationEvent;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.RecordControlRequestedEvent;

/** Bridge ApplicationEventBus -> RecorderCoordinator. */
public final class RecorderEventBridge {

    private static volatile boolean installed = false;

    private RecorderEventBridge() {}

    /** Install exactly once to forward START/STOP/TOGGLE to the coordinator. */
    public static synchronized void install() {
        if (installed) return;
        ApplicationEventBus.getInstance().subscribe(new java.util.function.Consumer<ApplicationEvent<?>>() {
            @Override public void accept(ApplicationEvent<?> ev) {
                if (!(ev instanceof RecordControlRequestedEvent)) return;

                RecordControlRequestedEvent.Payload p =
                        ((RecordControlRequestedEvent) ev).getPayload();

                RecorderCoordinator coord = RecorderCoordinator.getInstance();
                String user = p.getUsername();

                switch (p.getOperation()) {
                    case START:
                        if (user != null) coord.startForUser(user);
                        else coord.startActiveRecording();
                        break;
                    case STOP:
                        if (user != null) coord.stopForUser(user);
                        else coord.stopActiveRecording();
                        break;
                    case TOGGLE:
                        if (user != null) coord.toggleForUser(user);
                        else coord.toggleActiveRecording();
                        break;
                    default:
                        // Do nothing
                }
            }
        });
        installed = true;
    }
}
