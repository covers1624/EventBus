package net.covers1624.eventbus;

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Type;

/**
 * Root class for all Generics filtered events.
 * <p>
 * Created by covers1624 on 10/4/21.
 */
@ApiStatus.Experimental // Not yet implemented.
public interface GenericEvent<T> extends Event {

    Type getGenericType();
}
