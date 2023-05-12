package net.covers1624.eventbus;

import net.covers1624.eventbus.internal.EventBusImpl;

import java.util.function.Consumer;

/**
 * A bus capable of registering functions/lambdas for receiving events of specific types.
 * <p>
 * Created by covers1624 on 22/3/21.
 */
public interface EventBus {

    /**
     * Create an {@link EventBus}.
     * <p>
     * The default {@link Environment} extension provided to this
     * bus should be sufficient for simple applications, where all
     * resources and classes are on the same classpath the event bus
     * is on. If your environment has custom requirements, you may use
     * {@link #create(Environment)} specifying a custom environment.
     *
     * @return The {@link EventBus} instance.
     */
    static EventBus create() {
        return new EventBusImpl();
    }

    /**
     * Create an {@link EventBus} instance with a custom environment.
     *
     * @param env The {@link Environment} to use.
     * @return The {@link EventBus}.
     */
    static EventBus create(Environment env) {
        return new EventBusImpl(env);
    }

    /**
     * Construct an {@link EventFactory} instance for firing the given event.
     * <p>
     * The provided {@link EventFactory} class must be the single enclosed {@link EventFactory}
     * class of the provided {@link Event} class.
     *
     * @param factoryClass The {@link EventFactory} of the event.
     * @param eventClass   The {@link Event} class.
     * @return A constructed Factory, capable of firing events for the given event class.
     */
    <T extends EventFactory> T constructFactory(Class<T> factoryClass, Class<? extends Event> eventClass);

    /**
     * Registers the given object searching for {@link SubscribeEvent} annotations on the owned members.
     * <p>
     * If an instance of some class is provided, the bus will search for {@code public} mon-static methods
     * declared on the provided type.
     * <p>
     * If an instance of a classes reflection {@link Class} object is provided, the bus will search for
     * {@code public static} methods declared on the provided type.
     * <p>
     * Any declared method discovered by the above processes is registered in one of two ways.<br/>
     * Methods consuming a {@link Event} class are registered as 'event consumers', these methods will
     * receive an instance of the {@link Event} object when fired.<br/>
     * Methods consuming a list of exploded event parameters corresponding to the event fields are
     * registered as a 'fast invoke' consumer, these methods will only receive the declared event fields.
     * In the case of these 'fast invoke' consumers, it is expected that an {@link Event} class reference is
     * provided via the {@link SubscribeEvent#value()} annotation field.
     *
     * @param object The object to register.
     */
    void register(Object object);

    /**
     * An overload of {@link #registerListener(Class, EventPriority, EventListener)} declaring normal priority.
     *
     * @param listenerType The Factory class associated with the event.
     * @param func         The Lambda or Method reference.
     */
    default <T extends EventListener> void registerListener(Class<T> listenerType, T func) {
        registerListener(listenerType, EventPriority.NORMAL, func);
    }

    /**
     * Registers a {@link EventListener} for the given event.
     * <p>
     * As opposed to {@link #registerListener(Class, Consumer)}, this method
     * registers a parameter based listener, which for most cases are the preferred
     * way to register an event.
     *
     * @param listenerType The Factory class associated with the event.
     * @param priority     The priority of this listener.
     * @param func         The Lambda or Method reference.
     */
    <T extends EventListener> void registerListener(Class<T> listenerType, EventPriority priority, T func);

    /**
     * An overload of {@link #registerListener(Class, EventPriority, Consumer)} declaring normal priority.
     *
     * @param eventClass The {@link Event} class to register for.
     * @param func       The Lambda or Method reference.
     */
    default <T extends Event> void registerListener(Class<T> eventClass, Consumer<T> func) {
        registerListener(eventClass, EventPriority.NORMAL, func);
    }

    /**
     * Registers a regular {@link Consumer} for the given event.
     * <p>
     * This lambda will receive full {@link Event} instances, opposed to just the
     * event fields.
     *
     * @param eventClass The {@link Event} class to register for.
     * @param priority   The priority of this listener.
     * @param func       The Lambda or Method reference.
     */
    <T extends Event> void registerListener(Class<T> eventClass, EventPriority priority, Consumer<T> func);

}
