package net.covers1624.eventbus.util;

/**
 * Created by covers1624 on 11/4/21.
 */
public class AccessibleClassLoader extends ClassLoader {

    public AccessibleClassLoader() {
        super();
    }

    public AccessibleClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
