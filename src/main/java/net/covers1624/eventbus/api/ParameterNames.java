package net.covers1624.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the names for all the annotated methods parameters.
 * <p>
 * This can be used if the method is not a Java method.
 * <p>
 * Created by covers1624 on 9/4/21.
 */
@Target (ElementType.METHOD)
@Retention (RetentionPolicy.RUNTIME)
public @interface ParameterNames {

    /**
     * @return The parameter names for the method.
     */
    String[] value();
}
