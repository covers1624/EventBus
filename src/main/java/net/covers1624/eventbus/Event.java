package net.covers1624.eventbus;

/**
 * Root of all event classes.
 * <p>
 * Events are simple interfaces with getters and setter functions describing a set of event fields.
 * <p>
 * Each method name will be syntactically parsed to match with its getter/setter sibling, building a set of
 * fields, with an associated getter, optional setter, and a type.
 * <p>
 * Getter methods are expected top be prefixed with either {@code get} or {@code is}, must have no parameters
 * and return the same type as the described field.
 * <p>
 * Setter methods are expected to be prefixed with {@code set} and contain a single parameter the same as
 * the field type. Events may choose to omit their setter, in this case, the field is considered reference immutable.
 * The event system, may treat these fields specially for greater performance.
 * <p>
 * Each event is expected to have a single inner-class {@link EventFactory}.
 * <p>
 * Events may choose to have one or more {@link EventListener} inner-classes. These are used for lambda subscriptions.
 * <p>
 * Created by covers1624 on 22/3/21.
 *
 * @see EventFactory
 * @see EventListener
 */
public interface Event {
}
