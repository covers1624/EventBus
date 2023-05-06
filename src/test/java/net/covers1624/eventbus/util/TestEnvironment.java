package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Created by covers1624 on 16/9/22.
 */
public abstract class TestEnvironment implements Environment {

    private final AccessibleClassLoader cl = new ThreadContextClassLoader();

    public static TestEnvironment WITHOUT_CLASSES = new TestEnvironment() {
        @Override
        @Nullable
        public InputStream getClassStream(Class<?> clazz) {
            return null;
        }
    };

    public static TestEnvironment WITH_CLASSES = new TestEnvironment() {
        @Override
        @Nullable
        public InputStream getClassStream(Class<?> clazz) {
            return TestEnvironment.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
        }
    };

    @Override
    public Class<?> defineClass(String cName, byte[] bytes) {
        return cl.defineClass(cName, bytes);
    }
}
