package net.covers1624.eventbus.api;

import java.lang.reflect.Type;

/**
 * Root class for all Generics filtered events.
 * <p>
 * Created by covers1624 on 10/4/21.
 */
public interface GenericEvent<T> extends Event {

    Type getGenericType();
}
