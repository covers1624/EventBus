package net.covers1624.eventbus.api;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 22/3/21.
 */
public interface EventBus {

    /**
     * Construct an {@link EventFactory} instance for firing the given event.
     * <p>
     * The provided {@link EventFactory} class must be unique for a given {@link Event} class,
     * they may not be shared between events.
     *
     * @param factoryClass The Factory class to fire the event.
     * @param eventClass   The event class interface.
     * @return A constructed Factory, capable of firing events for the given event class.
     */
    <T extends EventFactory> T constructFactory(Class<T> factoryClass, Class<? extends Event> eventClass);

    /**
     * Registers the given object to the {@link EventBus}.
     * This can either be an instance or a Class object for static registrations.
     *
     * @param object The object.
     */
    void register(Object object);

    /**
     * Registers a lambda based Listener for the given event.
     * <p>
     * As opposed to {@link #registerListener(Class, Consumer)}, this method
     * registers a parameter based listener, which for most cases are the preferred
     * way to register an event.
     *
     * @param invoker The Factory class associated with the event.
     * @param lambda  The Lambda or Method reference.
     */
    <T extends EventListener<?>> void registerListener(Class<T> invoker, T lambda);

    <T extends Event> void registerListener(Class<T> factory, Consumer<T> event);

}
