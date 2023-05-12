package net.covers1624.eventbus.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.covers1624.eventbus.Environment;
import net.covers1624.eventbus.ParameterNames;
import net.covers1624.quack.collection.FastStream;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by covers1624 on 9/4/21.
 */
@ApiStatus.Internal
class MethodParamLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodParamLookup.class);

    private final Environment env;

    private final Map<Class<?>, Map<String, List<String>>> asmParamCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, List<String>>> metadataParamCache = new ConcurrentHashMap<>();
    private final Map<Method, List<String>> paramCache = new ConcurrentHashMap<>();

    public MethodParamLookup(Environment env) {
        this.env = env;
    }

    /**
     * Get the parameter names for a given {@link Method}.
     * <p>
     * First it will try and load the {@link ParameterNames} annotation on the method.
     * <p>
     * Next, it will try loading from the META-INF folder, with metadata extracted at compile time
     * via the accompanying annotation processor.
     * <p>
     * Finally, it will try loading the bytecode of the class, parsing out the local variable table.
     * <p>
     * If all these fail, an empty list of parameters will be returned.
     *
     * @param method The method to find parameters for.
     * @return The found parameter names in order. Otherwise, an empty list.
     * @see ParameterNames
     */
    public List<String> getMethodParams(Method method) {
        synchronized (method) {
            List<String> params = paramCache.get(method);
            if (params != null) return params;

            params = computeMethodParams(method);
            if (params == null) return ImmutableList.of();

            paramCache.put(method, params);
            return params;
        }
    }

    private List<String> computeMethodParams(Method method) {
        // Try loading via annotation.
        List<String> params = loadNamedParams(method);
        if (params != null) return params;

        String key = method.getName() + Type.getMethodDescriptor(method);

        // Try via AnnotationProcessor metadata.
        params = loadMetaParams(method.getDeclaringClass()).get(key);
        if (params != null) return params;

        // Try by reading the class bytecode.
        params = loadAsmParams(method.getDeclaringClass()).get(key);
        return params;
    }

    @Nullable
    private List<String> loadNamedParams(Method method) {
        ParameterNames names = method.getAnnotation(ParameterNames.class);
        if (names == null) return null;

        String[] nameStrings = names.value();
        if (nameStrings.length != method.getParameterCount()) {
            LOGGER.error("Method {}.{} must have Named annotation on all parameters.", method.getDeclaringClass().getName(), method.getName());
            return null;
        }

        return FastStream.of(nameStrings)
                .map(String::intern)
                .toImmutableList();
    }

    private Map<String, List<String>> loadMetaParams(Class<?> clazz) {
        synchronized (clazz) {
            Map<String, List<String>> params = metadataParamCache.get(clazz);
            if (params != null) return params;

            String resource = "/META-INF/eventbus/" + clazz.getName() + ".params";
            try (InputStream is = env.getResourceStream(resource)) {
                if (is == null) return ImmutableMap.of();

                params = new HashMap<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;

                    String[] split = line.split(" ");
                    if (split.length != 2) {
                        LOGGER.error("Error reading resource file {}. Invalid line: {}", resource, line);
                        continue;
                    }

                    params.put(
                            split[0].intern(), // Intern this too, could be quite a few of these.
                            FastStream.of(split[1].split(","))
                                    .map(String::intern) // Intern all the field names, there may be lots of these.
                                    .toImmutableList()
                    );
                }
                params = ImmutableMap.copyOf(params);
                metadataParamCache.put(clazz, params);
                return params;
            } catch (IOException ex) {
                LOGGER.error("Error parsing resource: " + resource, ex);
                return ImmutableMap.of();
            }
        }
    }

    private Map<String, List<String>> loadAsmParams(Class<?> clazz) {
        synchronized (clazz) {
            Map<String, List<String>> params = asmParamCache.get(clazz);
            if (params != null) return params;

            params = new HashMap<>();
            try (InputStream is = env.getClassStream(clazz)) {
                if (is != null) {
                    ClassReader reader = new ClassReader(is);
                    MethodParamClassVisitor visitor = new MethodParamClassVisitor(params);
                    reader.accept(visitor, ClassReader.SKIP_FRAMES);
                }
            } catch (IOException ex) {
                LOGGER.debug("Failed to read class: {}", clazz.getName(), ex);
            }
            params = ImmutableMap.copyOf(params);
            asmParamCache.put(clazz, params);
            return params;
        }
    }

    private static class MethodParamClassVisitor extends ClassVisitor {

        public final Map<String, List<String>> methodParamMap;

        public MethodParamClassVisitor(Map<String, List<String>> methodParamMap) {
            super(Opcodes.ASM9);
            this.methodParamMap = methodParamMap;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            int paramWidth = getParamWidth(descriptor);
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;

            List<String> params = new ArrayList<>();
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                    if ((index != 0 || isStatic) && index < (isStatic ? paramWidth : paramWidth + 1)) {
                        params.add(name.intern());
                    }
                }

                @Override
                public void visitEnd() {
                    if (!params.isEmpty()) {
                        methodParamMap.put((name + descriptor).intern(), ImmutableList.copyOf(params));
                    }
                }
            };
        }

        private static int getParamWidth(String desc) {
            int width = 0;
            for (Type arg : Type.getMethodType(desc).getArgumentTypes()) {
                width += arg.getSize();
            }
            return width;
        }
    }
}
