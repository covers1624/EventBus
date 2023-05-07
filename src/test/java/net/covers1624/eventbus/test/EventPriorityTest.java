package net.covers1624.eventbus.test;

import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.internal.EventBusImpl;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 7/5/23.
 */
public class EventPriorityTest extends TestBase {

    private static final EventBus BUS = new EventBusImpl(WITH_CLASSES);

    @Test
    public void doTest() {
        AtomicInteger i = new AtomicInteger();
        BUS.registerListener(SimpleEvent.Listener.class, EventPriority.NORMAL, () -> assertEquals(2, i.getAndIncrement()));
        BUS.registerListener(SimpleEvent.Listener.class, EventPriority.HIGHEST, () -> assertEquals(0, i.getAndIncrement()));
        BUS.registerListener(SimpleEvent.Listener.class, EventPriority.HIGH, () -> assertEquals(1, i.getAndIncrement()));
        BUS.registerListener(SimpleEvent.Listener.class, EventPriority.LOWEST, () -> assertEquals(5, i.getAndIncrement()));
        BUS.registerListener(SimpleEvent.Listener.class, EventPriority.LOW, () -> assertEquals(4, i.getAndIncrement()));
        // Test that sort is stable.
        BUS.registerListener(SimpleEvent.Listener.class, EventPriority.NORMAL, () -> assertEquals(3, i.getAndIncrement()));
        SimpleEvent.FACTORY.fire();
        assertEquals(6, i.get());
    }

    public interface SimpleEvent extends Event {

        Factory FACTORY = BUS.constructFactory(Factory.class, SimpleEvent.class);

        abstract class Factory extends EventFactory {

            public abstract void fire();
        }

        interface Listener extends EventListener {

            void fire();
        }
    }
}
