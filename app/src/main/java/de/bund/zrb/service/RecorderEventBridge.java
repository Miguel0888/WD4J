package de.bund.zrb.service;

import de.bund.zrb.event.ApplicationEvent;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.RecordControlRequestedEvent;
import de.bund.zrb.event.EventServiceControlRequestedEvent;

/** Bridge ApplicationEventBus → RecorderCoordinator. Install exactly once on app start. */
public final class RecorderEventBridge {

    private static volatile boolean installed = false;

    private RecorderEventBridge() {}

    /** Install bridge once so RecordControlRequestedEvent gets handled. */
    public static synchronized void install() {
        if (installed) return;

        ApplicationEventBus.getInstance().subscribe(new java.util.function.Consumer<ApplicationEvent<?>>() {
            @Override
            public void accept(ApplicationEvent<?> ev) {
                RecorderCoordinator coord = RecorderCoordinator.getInstance();
                // Handle record control events
                if (ev instanceof RecordControlRequestedEvent) {
                    RecordControlRequestedEvent.Payload p = ((RecordControlRequestedEvent) ev).getPayload();
                    String user = p.getUsername();
                    switch (p.getOperation()) {
                        case START:
                            if (user != null) coord.startForUser(user); else coord.startActiveRecording();
                            return;
                        case STOP:
                            if (user != null) coord.stopForUser(user); else coord.stopActiveRecording();
                            return;
                        case TOGGLE:
                            if (user != null) coord.toggleForUser(user); else coord.toggleActiveRecording();
                            return;
                        default:
                            return;
                    }
                }
                // Handle event service control events
                if (ev instanceof EventServiceControlRequestedEvent) {
                    EventServiceControlRequestedEvent.Payload p = ((EventServiceControlRequestedEvent) ev).getPayload();
                    String user = p.getUsername();
                    switch (p.getOperation()) {
                        case START:
                            if (user != null) coord.startEventsForUser(user); else coord.startEventsForActiveTab();
                            break;
                        case STOP:
                            if (user != null) coord.stopEventsForUser(user); else coord.stopEventsForActiveTab();
                            break;
                        case TOGGLE:
                            // Simple toggle: if a user is specified, attempt to stop; if not running, start
                            if (user != null) {
                                coord.stopEventsForUser(user);
                                coord.startEventsForUser(user);
                            } else {
                                coord.stopEventsForActiveTab();
                                coord.startEventsForActiveTab();
                            }
                            break;
                        default:
                            break;
                    }
                    return;
                }
            }
        });

        installed = true;
    }
}
