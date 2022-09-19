package net.covers1624.eventbus.util;

/**
 * A simple {@link ClassLoader} which delegates all class loading to
 * the current {@link Thread#getContextClassLoader()}.
 * <p>
 * Created by covers1624 on 18/9/22.
 */
public class ThreadContextClassLoader extends AccessibleClassLoader {

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return Class.forName(name, resolve, Thread.currentThread().getContextClassLoader());
    }
}
