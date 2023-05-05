package net.covers1624.eventbus.internal;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.util.MethodParamLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private final BiMap<Class<? extends EventFactory<?>>, Class<? extends Event>> factoryToEvent = Maps.synchronizedBiMap(HashBiMap.create());
    private final BiMap<Class<? extends Event>, Class<? extends EventFactory<?>>> eventToFactory = factoryToEvent.inverse();

    private final Map<Class<? extends Event>, EventListenerList> eventLists = new ConcurrentHashMap<>();

    public EventBusImpl(Environment environment) {
        this.environment = environment;
        paramLookup = new MethodParamLookup(environment);
    }

    @Override
    public <T extends EventFactory<T>> T constructFactory(Class<T> factoryClass, Class<? extends Event> eventClass) {
        if (eventClass.equals(factoryToEvent.get(factoryClass)) && factoryClass.equals(eventToFactory.get(eventClass))) {
            // We have already made a factory for this event, just return it again.
            //noinspection unchecked
            return (T) getListenerList(eventClass).getRootFactory();
        }
        checkNotAlreadyRegistered(factoryClass, eventClass);
        factoryToEvent.put(factoryClass, eventClass);

        EventListenerList event = getListenerList(eventClass);
        event.bindFactory(factoryClass);

        //noinspection unchecked
        return (T) event.getRootFactory();
    }

    private EventListenerList getListenerList(Class<? extends Event> clazz) {
        return eventLists.computeIfAbsent(clazz, e -> new EventListenerList(this, clazz));
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

        for (Method method : clazz.getDeclaredMethods()) {
            LOGGER.info("Considering method {} for event subscription.", method);
            if (method.getModifiers() != methodFilter) {
                LOGGER.debug("Method not applicable, invalid flags. ");
                continue;
            }
            tryRegisterMethod(object, method);
        }
    }

    private void tryRegisterMethod(Object object, Method method) {
        SubscribeEvent sub = method.getDeclaredAnnotation(SubscribeEvent.class);
        if (sub == null) {
            LOGGER.debug("Method does not have SubscribeEvent annotation.");
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

            LOGGER.info(" Registered event class listener {} for {}.", method, paramEvent.getName());
            getListenerList(paramEvent)
                    .registerMethod(object, method);
        } else if (annotationEvent != null) {
            List<String> params = paramLookup.findParameterNames(method);
            if (params.size() != args.length) {
                LOGGER.error("Unable to extract all method param names. " + method);
                return;
            }

            EventListenerList eventList = getListenerList(annotationEvent);
            for (int i = 0; i < params.size(); i++) {
                String param = params.get(i);
                if (!eventList.fields.containsKey(param)) {
                    LOGGER.info("Method parameter not an event field. Param: '{}', Index: {}, Method: {}", param, i, method);
                    return;
                }
            }
            LOGGER.info(" Registered fast invoke event listener {} for {}.", method, annotationEvent.getName());
            eventList.registerMethod(object, method, params);
        } else {
            LOGGER.debug(" Unable to determine subscribed event. " + method);
        }
    }

    @Override
    public <T extends EventListener<?>> void registerListener(Class<T> listener, T lambda) {
        EventListenerList list = getListenerList(EventListenerList.getEventForListener(listener));
        if (list == null) throw new IllegalArgumentException(String.format("No event with listener '%s' is registered.", listener.getName()));

        LOGGER.info("Registered fast invoke lambda event listener for {}.", list.eventInterface.getName());
        list.registerListener(listener, lambda);
    }

    @Override
    public <T extends Event> void registerListener(Class<T> eventClass, Consumer<T> cons) {
        EventListenerList list = getListenerList(eventClass);
        if (list == null) throw new IllegalArgumentException(String.format("No event with class '%s' is registered.", eventClass.getName()));

        LOGGER.info("Registered event class lambda event listener for {}.", list.eventInterface.getName());
        list.registerEventConsumer(cons);
    }

    private void checkNotAlreadyRegistered(Class<? extends EventFactory<?>> factoryClass, Class<? extends Event> eventClass) {
        Class<? extends Event> registeredEvent = factoryToEvent.get(factoryClass);
        if (registeredEvent != null) {
            throw new IllegalArgumentException(String.format("Event factory '%s' is already registered for event '%s'.", factoryClass.getName(), registeredEvent.getName()));
        }
        Class<? extends EventFactory<?>> registeredFactory = eventToFactory.get(eventClass);
        if (registeredFactory != null) {
            throw new IllegalArgumentException(String.format("Event class '%s' is already registered with factory '%s'.", eventClass.getName(), registeredFactory.getName()));
        }
    }
}
