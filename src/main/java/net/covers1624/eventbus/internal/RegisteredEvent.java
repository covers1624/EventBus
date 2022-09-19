package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.api.EventInvoker;
import net.covers1624.eventbus.util.EventField;
import net.covers1624.eventbus.util.EventFieldExtractor;
import net.covers1624.eventbus.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 17/9/22.
 */
class RegisteredEvent {

    private static final Method CONS_METHOD = Utils.getSingleMethod(Consumer.class);

    final EventBusImpl bus;
    public final Class<? extends Event> eventInterface;
    public final Class<? extends EventInvoker> eventInvoker;
    public final Map<String, EventField> fields;

    final Method invokerMethod;
    final List<String> invokerParams;

    final EventListenerList listenerList;

    public RegisteredEvent(EventBusImpl bus, Class<? extends Event> eventInterface, Class<? extends EventInvoker> eventInvoker) {
        assert eventInterface.isInterface();
        assert Event.class.isAssignableFrom(eventInterface);

        this.bus = bus;
        this.eventInterface = eventInterface;
        this.eventInvoker = eventInvoker;
        this.fields = EventFieldExtractor.getEventFields(eventInterface);

        invokerMethod = Utils.getSingleMethod(eventInvoker);
        invokerParams = bus.paramLookup.findParameterNames(invokerMethod);

        listenerList = (EventListenerList) ListenerListDecorator.generate(this);
    }

    public <T> T getRootInvoker() {
        return unsafeCast(listenerList);
    }

    public void registerMethod(@Nullable Object obj, Method method, List<String> params) {
        listenerList.listeners.add(new ListenerHandle(obj, method, params));
        listenerList.invalid = true;
    }

    public void registerMethod(@Nullable Object obj, Method method) {
        listenerList.listeners.add(new ListenerHandle(obj, method, null));
        listenerList.invalid = true;
    }

    public void registerListener(Object lambda) {
        listenerList.listeners.add(new ListenerHandle(lambda, invokerMethod, invokerParams));
        listenerList.invalid = true;
    }

    public void registerEventConsumer(Consumer<?> cons) {
        listenerList.listeners.add(new ListenerHandle(cons, CONS_METHOD, null));
        listenerList.invalid = true;
    }
}
