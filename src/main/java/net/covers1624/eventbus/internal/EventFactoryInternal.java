package net.covers1624.eventbus.internal;

import org.jetbrains.annotations.ApiStatus;

/**
 * Created by covers1624 on 19/9/22.
 */
@ApiStatus.Internal
public interface EventFactoryInternal {

    void setDirty();

    boolean isDirty();

    void setFactory(Object eventFactory);
}
