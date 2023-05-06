package net.covers1624.eventbus.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker annotation for marking methods for {@link EventBus#register}
 * <p>
 * Created by covers1624 on 22/3/21.
 */
@Target (METHOD)
@Retention (RUNTIME)
public @interface SubscribeEvent {

    /**
     * The event class to subscribe to.
     * <p>
     * Only one element may be provided.
     * <p>
     * If no elements are provided, it will attempt to infer the event class from the
     * annotated member.
     *
     * @return The event class.
     */
    Class<? extends Event>[] value() default {};

    /**
     * The Priority of this handler.
     *
     * @return The priority.
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Controls if this listener listens to canceled events.
     *
     * @return If canceled events should be handled anyway.
     */
    boolean receiveCanceled() default false;

}
