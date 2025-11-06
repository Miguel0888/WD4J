package de.bund.zrb.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Simple app-wide event bus with typed and untyped subscriptions. */
public final class ApplicationEventBus {

    private static final ApplicationEventBus INSTANCE = new ApplicationEventBus();
    public static ApplicationEventBus getInstance() { return INSTANCE; }

    private final List<Consumer<ApplicationEvent<?>>> anyListeners = new CopyOnWriteArrayList<Consumer<ApplicationEvent<?>>>();
    private final Map<Class<?>, List<Consumer<?>>> typed = new ConcurrentHashMap<Class<?>, List<Consumer<?>>>();

    private ApplicationEventBus() { }

    /** Backward-compatible: receive all events. */
    public void subscribe(Consumer<ApplicationEvent<?>> listener) {
        if (listener != null) anyListeners.add(listener);
    }

    /** Typed subscribe: receive only events assignable to clazz. */
    public <T extends ApplicationEvent<?>> void subscribe(Class<T> clazz, Consumer<T> listener) {
        if (clazz == null || listener == null) return;
        List<Consumer<?>> list = typed.computeIfAbsent(clazz, k -> new CopyOnWriteArrayList<Consumer<?>>());
        list.add(listener);
    }

    public void unsubscribe(Consumer<ApplicationEvent<?>> listener) {
        anyListeners.remove(listener);
    }

    public <T extends ApplicationEvent<?>> void unsubscribe(Class<T> clazz, Consumer<T> listener) {
        if (clazz == null || listener == null) return;
        List<Consumer<?>> list = typed.get(clazz);
        if (list != null) list.remove(listener);
    }

    public void publish(ApplicationEvent<?> event) {
        if (event == null) return;

        // 1) fan-out to untyped listeners
        for (Consumer<ApplicationEvent<?>> l : anyListeners) {
            try { l.accept(event); } catch (Throwable ignore) { }
        }

        // 2) fan-out to typed listeners (isAssignableFrom)
        for (Map.Entry<Class<?>, List<Consumer<?>>> e : typed.entrySet()) {
            Class<?> key = e.getKey();
            if (key.isInstance(event)) {
                for (Consumer<?> raw : e.getValue()) {
                    @SuppressWarnings("unchecked")
                    Consumer<ApplicationEvent<?>> l = (Consumer<ApplicationEvent<?>>) raw;
                    try { l.accept(event); } catch (Throwable ignore) { }
                }
            }
        }
    }
}
