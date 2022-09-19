package net.covers1624.eventbus.internal;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by covers1624 on 17/9/22.
 */
public class ListenerHandle {

    @Nullable
    public final Object instance;
    public final Method handle;
    @Nullable
    private final List<String> params;

    public ListenerHandle(@Nullable Object instance, Method handle, @Nullable List<String> params) {
        this.instance = instance;
        this.handle = handle;
        this.params = params != null ? ImmutableList.copyOf(params) : null;
    }

    public boolean isFastInvoke() {
        return params != null;
    }

    public List<String> getParams() {
        return params;
    }
}
