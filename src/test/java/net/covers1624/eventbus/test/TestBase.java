package net.covers1624.eventbus.test;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.util.AccessibleClassLoader;
import net.covers1624.eventbus.util.ThreadContextClassLoader;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Created by covers1624 on 8/5/23.
 */
public abstract class TestBase {

    static {
        System.setProperty("net.covers1624.eventbus.debug", "true");
    }

    private static final AccessibleClassLoader cl = new ThreadContextClassLoader();

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
            return TestBase.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
        }
    };

    public static abstract class TestEnvironment implements Environment {

        @Override
        public Class<?> defineClass(String cName, byte[] bytes) {
            return cl.defineClass(cName, bytes);
        }
    }
}
