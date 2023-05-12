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

    public static TestEnvironment WITHOUT_RESOURCES = new TestEnvironment() {
        @Override
        @Nullable
        public InputStream getClassStream(Class<?> clazz) {
            return null;
        }

        @Override
        @Nullable
        public InputStream getResourceStream(String resource) {
            return null;
        }
    };
    public static TestEnvironment WITH_RESOURCES = new TestEnvironment() {
        @Override
        @Nullable
        public InputStream getClassStream(Class<?> clazz) {
            return TestBase.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
        }

        @Override
        @Nullable
        public InputStream getResourceStream(String resource) {
            return TestBase.class.getResourceAsStream(resource);
        }
    };

    public static abstract class TestEnvironment implements Environment {

        @Override
        public Class<?> defineClass(String cName, byte[] bytes) {
            return cl.defineClass(cName, bytes);
        }
    }
}
