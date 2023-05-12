package net.covers1624.eventbus.api;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Represents any abstractions required for running EventBus in
 * environments with different requirements.
 * <p>
 * Created by covers1624 on 24/7/22.
 */
public interface Environment {

    /**
     * If EventBus Debug is enabled.
     * <p>
     * This may enable verbose logging, dumping of generated classes, etc.
     */
    boolean DEBUG = Boolean.getBoolean("net.covers1624.eventbus.debug");

    /**
     * Get the bytes for the specified class.
     * <p>
     * This is used for analyzing the declared methods and extracting
     * local variable table information.
     *
     * @param clazz The bytes of the class.
     * @return The classes bytes.
     */
    @Nullable
    InputStream getClassStream(Class<?> clazz);

    /**
     * Get the stream for a resource.
     * <p>
     * This is used for reading META-INF parameter mapping files.
     *
     * @param resource The resource file.
     * @return The resource stream.
     */
    @Nullable
    InputStream getResourceStream(String resource);

    /**
     * Define the given class.
     *
     * @param cName The name of the class.
     * @param bytes The bytes of the class.
     * @return The defined class.
     */
    Class<?> defineClass(String cName, byte[] bytes);
}
