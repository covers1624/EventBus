package net.covers1624.eventbus.util;

import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.io.IOUtils;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static String simpleName(Class<?> clazz) {
        String str = clazz.getName();
        int lastDot = str.lastIndexOf('.');
        return lastDot != -1 ? str.substring(lastDot + 1) : str;
    }

    public static Type synClassName(Class<?> base, String desc, Class<?> extension, AtomicInteger counter) {
        return Type.getObjectType(asmName(base) + "$$" + desc + "$$" + simpleName(extension) + "$$" + counter.getAndIncrement());
    }

    public static Method getSingleAbstractMethod(Class<?> clazz) {
        return StreamableIterable.of(clazz.getMethods())
                .filter(e -> (e.getModifiers() & Modifier.ABSTRACT) != 0)
                .only();
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
