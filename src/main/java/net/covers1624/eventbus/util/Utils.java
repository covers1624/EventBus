package net.covers1624.eventbus.util;

import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.io.IOUtils;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by covers1624 on 17/9/22.
 */
public class Utils {

    public static String asmName(Class<?> clazz) {
        return asmName(clazz.getName());
    }

    public static String asmName(String name) {
        return name.replace(".", "/");
    }

    public static Type getObjectType(String name) {
        return Type.getObjectType(name.replace('.', '/'));
    }

    public static Method getSingleMethod(Class<?> clazz) {
        assert clazz.isInterface();

        return StreamableIterable.of(clazz.getMethods())
                .filter(e -> (e.getModifiers() == (Modifier.PUBLIC | Modifier.ABSTRACT)))
                .only();
    }

    public static Constructor<?> singleConstructor(Class<?> clazz) {
        return clazz.getConstructors()[0];
    }

    public static void debugWriteClass(String cName, byte[] bytes) {
        try {
            Path output = Paths.get("asm/eventbus/" + cName + ".class");
            try (OutputStream os = Files.newOutputStream(IOUtils.makeParents(output))) {
                os.write(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
    }
}
