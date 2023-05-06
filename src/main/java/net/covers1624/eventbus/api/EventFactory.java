package net.covers1624.eventbus.api;

/**
 * Responsible for declaring functions for invoking an event.
 * <p>
 * Each {@link Event} instance is expected to have a single enclosed {@link EventFactory} class with
 * a single {@code public abstract} function for firing the event.
 * <p>
 * The event system will generate an implementation of this class via {@link EventBus#constructFactory}.
 * <p>
 * Factories may choose to declare one or more functions for firing an event, delegating to the next. Binary
 * compatibility can be maintained using this.
 * <p>
 * Created by covers1624 on 19/9/22.
 *
 * @see Event
 * @see EventListener
 */
public abstract class EventFactory {
}
