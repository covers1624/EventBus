package net.covers1624.eventbus.internal;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.util.MethodParamLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 9/4/21.
 */
public class EventBusImpl implements EventBus {

    private static final Logger LOGGER = LogManager.getLogger();

    final Environment environment;
    final MethodParamLookup paramLookup;

    private final BiMap<Class<? extends EventInvoker>, Class<? extends Event>> invokerToEvent = HashBiMap.create();
    private final BiMap<Class<? extends Event>, Class<? extends EventInvoker>> eventToInvoker = invokerToEvent.inverse();

    private final Map<Class<? extends Event>, RegisteredEvent> registeredEvents = new ConcurrentHashMap<>();

    public EventBusImpl(Environment environment) {
        this.environment = environment;
        paramLookup = new MethodParamLookup(environment);
    }

    @Override
    public <T extends EventInvoker> T registerEvent(Class<T> factoryClass, Class<? extends Event> eventClass) {
        // Preconditions
        if (invokerToEvent.containsKey(factoryClass)) {
            throw new IllegalArgumentException(String.format("Event factory '%s' is already registered for event '%s'.", factoryClass.getName(), invokerToEvent.get(factoryClass).getName()));
        }
        if (eventToInvoker.containsKey(eventClass)) {
            throw new IllegalArgumentException(String.format("Event class '%s' is already registered with factory '%s'.", eventClass.getName(), eventToInvoker.get(eventClass).getName()));
        }
        invokerToEvent.put(factoryClass, eventClass);

        RegisteredEvent event = new RegisteredEvent(this, eventClass, factoryClass);
        registeredEvents.put(eventClass, event);

        return event.getRootInvoker();
    }

    @Nullable
    private RegisteredEvent getRegisteredEvent(Class<?> clazz) {
        RegisteredEvent registeredEvent = registeredEvents.get(clazz);
        if (registeredEvent == null) {
            Class<?> eventClass = invokerToEvent.get(clazz);
            if (eventClass != null) {
                registeredEvent = registeredEvents.get(eventClass);
            }
        }

        return registeredEvent;
    }

    @Override
    public void register(Object object) {
        Class<?> clazz = object instanceof Class ? (Class<?>) object : object.getClass();
        boolean isStatic = clazz == object;
        int methodFilter = Modifier.PUBLIC;
        if (isStatic) {
            object = null;
            methodFilter |= Modifier.STATIC;
        }

        for (Method method : clazz.getMethods()) {
            LOGGER.info("Considering method {} for event subscription.", method);
            if (method.getModifiers() != methodFilter) {
                LOGGER.info("Method not applicable, invalid flags. ");
                continue;
            }
            tryRegisterMethod(object, method);
        }
    }

    private void tryRegisterMethod(Object object, Method method) {
        SubscribeEvent sub = method.getDeclaredAnnotation(SubscribeEvent.class);
        if (sub == null) {
            LOGGER.info("Method does not have SubscribeEvent annotation.");
            return;
        }
        Class<? extends Event>[] events = sub.value();
        if (events.length > 1) throw new IllegalStateException("Unable to register to more than one event. Method: " + method);

        Class<?>[] args = method.getParameterTypes();
        Class<? extends Event> annotationEvent = events.length == 1 ? events[0] : null;
        Class<? extends Event> paramEvent = args.length == 1 && Event.class.isAssignableFrom(args[0]) ? unsafeCast(args[0]) : null;

        if (paramEvent != null) {
            // First and only parameter is an Event, register as an event consumer.
            if (annotationEvent != null && !paramEvent.equals(annotationEvent)) {
                LOGGER.error("Parameter and Annotation disagree on event type.");
                return;
            }

            LOGGER.info("Detected event from method parameters: {}.", paramEvent);
            RegisteredEvent registeredEvent = registeredEvents.get(paramEvent);
            if (registeredEvent == null) {
                LOGGER.info("Event {} is not known by this event bus.", paramEvent);
                return;
            }
            registeredEvent.registerMethod(object, method);
        } else if (annotationEvent != null) {
            List<String> params = paramLookup.findParameterNames(method);
            if (params.size() != args.length) {
                LOGGER.error("Unable to extract all method param names. " + method);
                return;
            }
            RegisteredEvent registeredEvent = registeredEvents.get(annotationEvent);
            if (registeredEvent == null) {
                LOGGER.info("Event {} is not known by this event bus.", paramEvent);
                return;
            }

            for (int i = 0; i < params.size(); i++) {
                String param = params.get(i);
                if (!registeredEvent.fields.containsKey(param)) {
                    LOGGER.info("Method parameter not an event field. Param: '{}', Index: {}, Method: {}", param, i, method);
                    return;
                }
            }
            registeredEvent.registerMethod(object, method, params);
        } else {
            LOGGER.info("Unable to determine subscribed event. " + method);
        }
    }

    @Override
    public <T extends EventInvoker> void registerListener(Class<T> invoker, T lambda) {
        RegisteredEvent event = getRegisteredEvent(invoker);
        if (event == null) throw new IllegalArgumentException(String.format("No event with listener '%s' is registered.", invoker.getName()));

        event.registerListener(lambda);
    }

    @Override
    public <T extends Event> void registerListener(Class<T> eventClass, Consumer<T> cons) {
        RegisteredEvent event = getRegisteredEvent(eventClass);
        if (event == null) throw new IllegalArgumentException(String.format("No event with class '%s' is registered.", eventClass.getName()));

        event.registerEventConsumer(cons);
    }
}
