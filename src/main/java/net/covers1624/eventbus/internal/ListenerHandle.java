package net.covers1624.eventbus.internal;

import com.google.common.collect.ImmutableList;
import net.covers1624.eventbus.EventPriority;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by covers1624 on 17/9/22.
 */
@ApiStatus.Internal
class ListenerHandle {

    @Nullable
    public final Object instance;
    public final Method handle;
    public final EventPriority priority;
    @Nullable
    private final List<String> params;

    public ListenerHandle(@Nullable Object instance, Method handle, EventPriority priority, @Nullable List<String> params) {
        this.instance = instance;
        this.handle = handle;
        this.priority = priority;
        this.params = params != null ? ImmutableList.copyOf(params) : null;
    }

    public boolean isFastInvoke() {
        return params != null;
    }

    @Nullable
    public List<String> getParams() {
        return params;
    }
}
