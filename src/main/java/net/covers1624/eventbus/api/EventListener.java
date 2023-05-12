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
 * If working in Java, the builtin annotation processor can be leveraged to extract parameter names. If you
 * are not working in Java, you can use the {@link ParameterNames} method annotation to specify them.
 * <p>
 * Parameter names are used to match each parameter with its associated event field.
 * <p>
 * Created by covers1624 on 22/3/21.
 *
 * @see Event
 * @see EventFactory
 */
public interface EventListener {
}
