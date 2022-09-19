package net.covers1624.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the event field name for a given parameter.
 * <p>
 * When a parameter is annotated with this Annotation, all parameters of that method
 * must also be annotated, it is treated as a fatal error if they are not annotated.
 * <p>
 * Created by covers1624 on 9/4/21.
 */
@Target (ElementType.PARAMETER)
@Retention (RetentionPolicy.RUNTIME)
public @interface Named {

    /**
     * @return The name for the event field.
     */
    String value();
}
