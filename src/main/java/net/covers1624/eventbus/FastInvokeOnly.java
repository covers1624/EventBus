package net.covers1624.eventbus;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker annotation to denote {@link Event} classes which only support
 * fast invoke.
 * <p>
 * There could be many reasons for this such as the event being particularly expensive.
 * <p>
 * The event should document the reasons for this.
 * <p>
 * This is not compatible with {@link CancelableEvent}.
 * <p>
 * Created by covers1624 on 5/5/23.
 */
@Target (TYPE)
@Retention (RUNTIME)
public @interface FastInvokeOnly {
}
