package com.kjmaster.yield.event.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A lightweight, synchronous event bus for internal domain events.
 * Decouples Logic (Producers) from UI (Consumers).
 */
public class YieldEventBus {
    private final Map<Class<?>, List<Consumer<?>>> subscribers = new HashMap<>();

    public <T> void register(Class<T> eventType, Consumer<T> listener) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> void post(T event) {
        List<Consumer<?>> listeners = subscribers.get(event.getClass());
        if (listeners != null) {
            // Copy list to avoid ConcurrentModificationException if a listener unregisters during execution
            for (Consumer<?> listener : new ArrayList<>(listeners)) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }
}