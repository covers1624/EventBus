package net.covers1624.eventbus.test;

import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.api.EventBus;
import net.covers1624.eventbus.api.EventFactory;
import net.covers1624.eventbus.api.EventListener;
import net.covers1624.eventbus.internal.EventBusImpl;
import org.junit.jupiter.api.Test;

/**
 * Created by covers1624 on 12/5/23.
 */
public class EventFactoryReturnsEventTest extends TestBase {

    private static final EventBus BUS = new EventBusImpl(WITH_RESOURCES);

    @Test
    public void doTest() {
        BUS.registerListener(SimpleEvent.Listener.class, () -> { });
        BUS.registerListener(SimpleEvent.class, e -> { });
        SimpleEvent event = SimpleEvent.FACTORY.fire();
    }

    public interface SimpleEvent extends Event {

        SimpleEvent.Factory FACTORY = BUS.constructFactory(SimpleEvent.Factory.class, SimpleEvent.class);

        abstract class Factory extends EventFactory {

            public abstract SimpleEvent fire();
        }

        interface Listener extends EventListener {

            void fire();
        }
    }
}
