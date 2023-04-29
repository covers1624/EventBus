package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.api.EventFactory;
import net.covers1624.eventbus.api.EventListener;
import net.covers1624.eventbus.util.EventField;
import net.covers1624.eventbus.util.EventFieldExtractor;
import net.covers1624.eventbus.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 17/9/22.
 */
public class RegisteredEvent {

    private static final Method CONS_METHOD = Utils.getSingleAbstractMethod(Consumer.class);

    final EventBusImpl bus;
    public final Class<? extends Event> eventInterface;
    public final Map<String, EventField> fields;

    @Nullable
    private Class<? extends EventFactory<?>> eventFactory;
    private EventFactoryInternal rootFactory;

    final List<ListenerHandle> listeners = new LinkedList<>();

    public RegisteredEvent(EventBusImpl bus, Class<? extends Event> eventInterface) {
        assert eventInterface.isInterface();
        assert Event.class.isAssignableFrom(eventInterface);

        this.bus = bus;
        this.eventInterface = eventInterface;
        this.fields = EventFieldExtractor.getEventFields(eventInterface);

//        invokerMethod = Utils.getSingleMethod(eventFactory);
//        invokerParams = bus.paramLookup.findParameterNames(invokerMethod);
    }

    void bindFactory(Class<? extends EventFactory<?>> eventFactory) {
        this.eventFactory = eventFactory;
        rootFactory = (EventFactoryInternal) EventFactoryDecorator.generate(this);
    }

    EventFactory<?> getRootFactory() {
        return (EventFactory<?>) rootFactory;
    }

    public void rebuildEventList() {
        synchronized (this) {
            if (!rootFactory.isDirty()) return;

            rootFactory.setFactory(EventListenerGenerator.generateEventFactory(this));
        }
    }

    public Class<? extends EventFactory<?>> getEventFactory() {
        return Objects.requireNonNull(eventFactory, "Factory has not been bound yet?");
    }

    //    public <T> T getRootInvoker() {
//        return unsafeCast(listenerList);
//    }

    public void registerMethod(@Nullable Object obj, Method method, List<String> params) {
        listeners.add(new ListenerHandle(obj, method, params));
        if (rootFactory != null) {
            rootFactory.setDirty();
        }
    }

    public void registerMethod(@Nullable Object obj, Method method) {
        listeners.add(new ListenerHandle(obj, method, null));
        if (rootFactory != null) {
            rootFactory.setDirty();
        }
    }

    public void registerListener(Class<? extends EventListener<?>> listener, Object lambda) {
        Method method = Utils.getSingleAbstractMethod(listener);
        listeners.add(new ListenerHandle(lambda, method, bus.paramLookup.findParameterNames(method)));
        if (rootFactory != null) {
            rootFactory.setDirty();
        }
    }

    public void registerEventConsumer(Consumer<?> cons) {
        listeners.add(new ListenerHandle(cons, CONS_METHOD, null));
        if (rootFactory != null) {
            rootFactory.setDirty();
        }
    }

    public static Class<? extends Event> getEventForListener(Class<? extends EventListener<?>> listener) {
        Type[] genericInterfaces = listener.getGenericInterfaces();
        if (genericInterfaces.length != 1) {
            throw new IllegalStateException("EventListener '" + listener.getName() + "' should only extend EventListener.");
        }
        if (!(genericInterfaces[0] instanceof ParameterizedType)) {
            throw new IllegalStateException("EventListener '" + listener.getName() + "' must parameterize EventListener.");
        }

        //noinspection unchecked
        return (Class<? extends Event>) ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments()[0];
    }
}
