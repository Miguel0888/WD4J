package de.bund.zrb.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ApplicationEventBus {

    private static final ApplicationEventBus INSTANCE = new ApplicationEventBus();

    private ApplicationEventBus() {}

    public static ApplicationEventBus getInstance() {
        return INSTANCE;
    }

    private final List<Consumer<ApplicationEvent<?>>> listeners = new ArrayList<>();

    public void publish(ApplicationEvent<?> event) {
        for (Consumer<ApplicationEvent<?>> listener : listeners) {
            listener.accept(event);
        }
    }

    public void subscribe(Consumer<ApplicationEvent<?>> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<ApplicationEvent<?>> listener) {
        listeners.remove(listener);
    }
}
