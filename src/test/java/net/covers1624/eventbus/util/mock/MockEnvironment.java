package net.covers1624.eventbus.util.mock;

import net.covers1624.eventbus.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Created by covers1624 on 16/9/22.
 */
public abstract class MockEnvironment implements Environment {

    private final ClassDefiner definer = ClassDefiner.internalClassLoader();

    public static MockEnvironment WITHOUT_CLASSES = new MockEnvironment() {
        @Override
        @Nullable
        public InputStream getClassStream(Class<?> clazz) {
            return null;
        }
    };

    public static MockEnvironment WITH_CLASSES = new MockEnvironment() {
        @Override
        @Nullable
        public InputStream getClassStream(Class<?> clazz) {
            return MockEnvironment.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
        }
    };

    @Override
    public ClassDefiner getClassDefiner() {
        return definer;
    }
}
