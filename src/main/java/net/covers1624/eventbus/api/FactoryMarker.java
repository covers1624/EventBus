package net.covers1624.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by covers1624 on 9/4/21.
 */
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.SOURCE)
public @interface FactoryMarker {
}
