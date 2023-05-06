package net.covers1624.eventbus.api;

/**
 * An interface for event lambda subscriptions.
 * <p>
 * An {@link EventListener} is expected to have a single method defining
 * all parameters this {@link EventListener} wishes to expose to a consumer.
 * <p>
 * An {@link Event} class may declare more than one {@link EventListener} if it chooses,
 * it is expected that binary compatability is achieved via this mechanism, declaring a new
 * interface when additional event fields are added, deprecating the former.
 * <p>
 * Each parameter must be annotated with {@link Named} in order for the event system to match each
 * parameter with the associated event field.
 * // TODO Provide a NamedParameters annotation, which via an annotation processor dumps all the parameter names.
 * <p>
 * Created by covers1624 on 22/3/21.
 *
 * @see Event
 * @see EventFactory
 */
public interface EventListener {
}
