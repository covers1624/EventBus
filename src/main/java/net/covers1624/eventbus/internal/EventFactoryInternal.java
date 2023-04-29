package net.covers1624.eventbus.internal;

/**
 * Created by covers1624 on 19/9/22.
 */
public interface EventFactoryInternal {

    void setDirty();

    boolean isDirty();

    void setFactory(Object eventFactory);
}
