package net.covers1624.eventbus.internal;

import com.google.common.collect.ImmutableMap;
import net.covers1624.eventbus.Event;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for extracting {@link EventField} entries from a given Class.
 * <p>
 * Also performs all necessary validation of {@link Event} classes.
 * <p>
 * Created by covers1624 on 10/4/21.
 */
@ApiStatus.Internal
class EventFieldExtractor {

    // TODO use a Guava cache with access expiry.
    private final static Map<Class<?>, Map<String, EventField>> eventFields = new ConcurrentHashMap<>();

    /**
     * Gets all {@link EventField} classes for a given class and all
     * parent classes.
     *
     * @param clazz The Event class.
     * @return The {@link EventField}s
     */
    public static Map<String, EventField> getEventFields(Class<?> clazz) {
        if (!Event.class.isAssignableFrom(clazz)) return Collections.emptyMap();

        if (!clazz.isInterface()) throw new IllegalStateException("Expected interface. Got: " + clazz);

        synchronized (clazz) {
            Map<String, EventField> fields = eventFields.get(clazz);
            if (fields != null) return fields;

            Map<String, EventField> allFields = new LinkedHashMap<>();

            // Add all parent interfaces.
            for (Class<?> iFace : clazz.getInterfaces()) {
                Map<String, EventField> iFaceFields = getEventFields(iFace);
                for (String fName : iFaceFields.keySet()) {
                    EventField eField = iFaceFields.get(fName);
                    EventField existingField = allFields.get(fName);

                    if (existingField == eField) continue; // If this EventField already exists, skip.
                    if (existingField != null) {
                        // It may be worth re-evaluating this in the future. It should be safe to allow duplicates
                        //  provided they have the same types.
                        throw new IllegalStateException(String.format(
                                "Found duplicate Event field name '%s'. Declared in '%s' and '%s'.",
                                fName,
                                eField.getter.getDeclaringClass().getName(),
                                existingField.getter.getDeclaringClass().getName()
                        ));
                    }
                    allFields.put(fName, eField);
                }
            }

            // Compute fields.
            allFields.putAll(compute(clazz));

            fields = ImmutableMap.copyOf(allFields);
            eventFields.put(clazz, fields);
            return fields;
        }
    }

    private static Map<String, EventField> compute(Class<?> clazz) {
        Map<String, FieldBuilder> found = new LinkedHashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            // Static or private methods are ignored.
            // TODO, ignore non-abstract (default) methods?
            if (((Modifier.STATIC | Modifier.PRIVATE) & method.getModifiers()) != 0) continue;
            String name = method.getName();
            if (name.startsWith("get") || name.startsWith("is")) {
                int chop = name.startsWith("is") ? 2 : 3;
                String fieldName = Utils.uncapitalize(name.substring(chop));

                FieldBuilder builder = found.computeIfAbsent(fieldName, FieldBuilder::new);
                if (builder.getter != null) {
                    throw new IllegalStateException("Already found getter '" + clazz.getName() + "." + builder.getter.getName() + "'. Duplicate: " + clazz.getName() + "." + method.getName());
                }
                builder.getter = method;
                builder.setType(method.getReturnType());
                builder.setGenericType(method.getGenericReturnType());
            } else if (name.startsWith("set")) {
                String fieldName = Utils.uncapitalize(name.substring(3));

                FieldBuilder builder = found.computeIfAbsent(fieldName, FieldBuilder::new);
                if (builder.setter != null) {
                    throw new IllegalStateException("Already found setter '" + clazz.getName() + "." + builder.setter.getName() + "'. Duplicate: " + clazz.getName() + "." + method.getName());
                }

                if (!method.getReturnType().equals(Void.TYPE)) throw new IllegalStateException(String.format("Expected void return on setter: %s.%s.", clazz.getName(), method.getName()));

                Parameter[] parameters = method.getParameters();
                if (parameters.length != 1) {
                    throw new IllegalStateException(String.format("Expected single parameter for method %s.%s. Got: %s", clazz.getName(), method.getName(), parameters.length));
                }

                builder.setter = method;
                builder.setType(parameters[0].getType());
                builder.setGenericType(parameters[0].getParameterizedType());
            } else {
                throw new IllegalStateException("Unknown public method inside Event class. Expected 'get', 'is', or 'set' prefix. " + clazz.getName() + "." + name);
            }

        }
        Map<String, EventField> fields = new HashMap<>();
        for (Map.Entry<String, FieldBuilder> pairEntry : found.entrySet()) {
            FieldBuilder builder = pairEntry.getValue();
            if (builder.getter == null) throw new IllegalStateException(String.format("Getter for event field '%s' in class '%s' required.", builder.name, clazz.getName()));

            fields.put(pairEntry.getKey(), builder.build());
        }

        return fields;
    }

    private static class FieldBuilder {

        public String name;
        public Method getter;
        public Method setter;
        public Class<?> type;
        public Type genericType;

        public FieldBuilder(String name) {
            this.name = name;
        }

        public void setType(Class<?> type) {
            if (this.type == null) {
                this.type = type;
                return;
            }
            if (!this.type.equals(type)) {
                throw new IllegalStateException("Unexpected type. Got: " + type + ", Expected: " + this.type);
            }
        }

        private void setGenericType(Type genericType) {
            if (this.genericType == null) {
                this.genericType = genericType;
                return;
            }
            if (!this.genericType.equals(genericType)) {
                throw new IllegalStateException("Unexpected generic type. Got: " + genericType + ", Expected: " + this.genericType);
            }
        }

        public EventField build() {
            return new EventField(name, type, genericType, getter, setter);
        }
    }
}
