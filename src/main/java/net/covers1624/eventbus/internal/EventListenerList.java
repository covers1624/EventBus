package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.EventListener;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by covers1624 on 10/4/21.
 */
public abstract class EventListenerList implements EventListener {

    private final RegisteredEvent event;

    final List<ListenerHandle> listeners = new LinkedList<>();

    protected boolean invalid = true;

    protected EventListenerList(RegisteredEvent event) {
        this.event = event;
    }

    public void invalidate() {
        invalid = true;
    }

    /**
     * Implemented by ASM Decorated extension to set the synthetic invoker field.
     */
    protected abstract void setInvoker(Object generated);

    /**
     * Implemented by ASM Decorated extension to get the synthetic invoker field.
     */
    protected abstract Object getInvoker();

    /**
     * Called by ASM Decorated extension to synchronously generate the _real_ invoker
     */
    protected void generateList() {
        synchronized (event) {
            if (!invalid && getInvoker() != null) return;

            setInvoker(EventListenerGenerator.generateEventFactory(event));
            invalid = false;
        }
    }
}
