package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.util.EventField;
import net.covers1624.eventbus.util.EventFieldExtractor;
import net.covers1624.eventbus.util.Utils;
import net.covers1624.quack.collection.FastStream;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 17/9/22.
 */
public class EventListenerList {

    private static final Method CONS_METHOD = Utils.requireSingleAbstractMethod(Consumer.class);

    final EventBusImpl bus;
    public final Class<? extends Event> eventInterface;
    public final Map<String, EventField> fields;
    public final boolean onlyFastInvoke;
    @Nullable
    public final Class<? extends EventFactory> eventFactory;
    @Nullable
    public final Method factoryMethod;
    @Nullable
    private final EventFactoryInternal rootFactory;

    private boolean unsorted = true;
    private final List<ListenerHandle> listeners = new ArrayList<>();

    public EventListenerList(EventBusImpl bus, Class<? extends Event> eventInterface) {
        assert eventInterface.isInterface();
        assert Event.class.isAssignableFrom(eventInterface);

        this.bus = bus;
        this.eventInterface = eventInterface;
        fields = EventFieldExtractor.getEventFields(eventInterface);
        onlyFastInvoke = eventInterface.getAnnotation(FastInvokeOnly.class) != null;

        eventFactory = getEnclosedFactory(eventInterface);
        if (eventFactory != null) {
            factoryMethod = getFactoryMethod(eventFactory);
            rootFactory = (EventFactoryInternal) EventFactoryDecorator.generate(this);
        } else {
            factoryMethod = null;
            rootFactory = null;
        }
    }

    EventFactory getRootFactory() {
        return (EventFactory) rootFactory;
    }

    List<ListenerHandle> getListeners() {
        if (unsorted) {
            listeners.sort(Comparator.comparing(e -> e.priority));
        }
        return listeners;
    }

    public void rebuildEventList() {
        synchronized (this) {
            if (!rootFactory.isDirty()) return;

            rootFactory.setFactory(EventListenerGenerator.generateEventFactory(this));
        }
    }

    public void registerMethod(@Nullable Object obj, Method method, EventPriority priority, List<String> params) {
        addListener(new ListenerHandle(obj, method, priority, params));
    }

    public void registerEventConsumerMethod(@Nullable Object obj, Method method, EventPriority priority) {
        addListener(new ListenerHandle(obj, method, priority, null));
    }

    public void registerListener(Class<? extends EventListener> listener, EventPriority priority, Object lambda) {
        Method method = Utils.requireSingleAbstractMethod(listener);
        addListener(new ListenerHandle(lambda, method, priority, bus.paramLookup.getMethodParams(method)));
    }

    public void registerEventConsumerListener(EventPriority priority, Consumer<?> cons) {
        addListener(new ListenerHandle(cons, CONS_METHOD, priority, null));
    }

    private void addListener(ListenerHandle handle) {
        if (onlyFastInvoke && !handle.isFastInvoke()) {
            throw new UnsupportedOperationException("Event " + eventInterface.getName() + " is marked as FastInvokeOnly. Must register a lambda or exploded method.");
        }
        unsorted = true;
        listeners.add(handle);
        if (rootFactory != null) {
            rootFactory.setDirty();
        }
    }

    public static Class<? extends Event> getEventForListener(Class<? extends EventListener> listener) {
        Class<?> clazz = listener.getEnclosingClass();
        if (!Event.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("EventListener must be enclosed inside its Event class.");
        }
        //noinspection unchecked
        return (Class<? extends Event>) clazz;
    }

    @Nullable
    private static Class<? extends EventFactory> getEnclosedFactory(Class<? extends Event> eventClazz) {
        List<Class<?>> factories = FastStream.of(eventClazz.getClasses())
                .filter(EventFactory.class::isAssignableFrom)
                .toList();
        if (factories.isEmpty()) {
            return null;
        } else if (factories.size() > 1) {
            throw new IllegalArgumentException("Found more than one EventFactory class inside event " + eventClazz.getName() + " found: " + factories);
        }
        //noinspection unchecked
        return (Class<? extends EventFactory>) factories.get(0);
    }

    private Method getFactoryMethod(Class<? extends EventFactory> eventFactory) {
        Method method = Utils.getSingleAbstractMethod(eventFactory);
        if (method == null) {
            throw new IllegalArgumentException("Expected factory " + eventFactory.getName() + " to contain a single abstract method.");
        }

        List<String> factoryParams = bus.paramLookup.getMethodParams(method);
        for (String param : factoryParams) {
            if (!fields.containsKey(param)) {
                throw new IllegalArgumentException("Parameter " + param + " does not map to an event field.");
            }
        }

        Class<?> clazz = method.getReturnType();
        if (clazz != void.class && clazz != eventInterface) {
            throw new IllegalArgumentException("Expected factory " + eventFactory.getName() + " to return void or " + eventInterface.getName());
        }

        return method;
    }
}
