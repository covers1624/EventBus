package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.api.Named;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by covers1624 on 9/4/21.
 */
public class MethodParamLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodParamLookup.class);

    private final Environment env;

    private final Map<Class<?>, Map<String, List<String>>> classParamCache = new HashMap<>();
    private final Map<Method, List<String>> foundParams = new ConcurrentHashMap<>();

    public MethodParamLookup(Environment env) {
        this.env = env;
    }

    /**
     * Finds the parameter names for a given {@link Method}.
     * <p>
     * By default, this will extract the parameter names directly from the local variable
     * table in bytecode.
     * <p>
     * Alternatively these names can be manually overridden with {@link Named}, which requires
     * all parameters in the method to be annotated.
     *
     * @param method The method to find parameters for.
     * @return The found parameter names in order. Otherwise, literal <code>null</code>
     * if the parameters could not be found.
     * @see Named
     */
    @Nullable
    public List<String> findParameterNames(Method method) {
        List<String> params = foundParams.get(method);
        if (params != null) return params;
        synchronized (foundParams) {
            params = foundParams.get(method);
            if (params != null) return params;

            Map<String, List<String>> methodParamMap = getParametersForClass(method.getDeclaringClass());

            params = methodParamMap.getOrDefault(method.getName() + Type.getMethodDescriptor(method), Collections.emptyList());
            foundParams.put(method, params);
            return params;
        }
    }

    private Map<String, List<String>> getParametersForClass(Class<?> clazz) {
        Map<String, List<String>> paramMap = classParamCache.get(clazz);
        if (paramMap != null) return paramMap;
        synchronized (classParamCache) {
            paramMap = classParamCache.get(clazz);
            if (paramMap != null) return paramMap;

            paramMap = new HashMap<>();
            // Parse All params using ASM.
            paramMap.putAll(ASMParameterParser.parseParams(env, clazz));
            // Overwrite with Named.
            paramMap.putAll(loadNamedParams(clazz));

            classParamCache.put(clazz, paramMap);
            return paramMap;
        }
    }

    private Map<String, List<String>> loadNamedParams(Class<?> clazz) {
        Map<String, List<String>> paramMap = new HashMap<>();

        outer:
        for (Method method : clazz.getDeclaredMethods()) {
            List<String> params = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                Named named = parameter.getAnnotation(Named.class);
                if (named == null) {
                    if (!params.isEmpty()) {
                        LOGGER.error("Method {}.{} must have Named annotation on all parameters.", clazz.getName(), method.getName());
                    }
                    continue outer;
                }
                params.add(named.value());
            }
            paramMap.put(method.getName() + Type.getMethodDescriptor(method), params);
        }
        return paramMap;
    }
}
