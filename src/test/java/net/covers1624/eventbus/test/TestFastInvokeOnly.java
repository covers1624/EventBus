package net.covers1624.eventbus.test;

import net.covers1624.eventbus.*;
import net.covers1624.eventbus.internal.EventBusImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by covers1624 on 5/5/23.
 */
public class TestFastInvokeOnly extends TestBase {

    public static final EventBus BUS = new EventBusImpl(WITH_RESOURCES);

    @Test
    public void doTest() {
        BUS.registerListener(SomeEvent.Listener.class, () -> {
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            BUS.registerListener(SomeEvent.class, e -> {
            });
        });
    }

    @FastInvokeOnly
    public interface SomeEvent extends Event {

        abstract class Factory extends EventFactory {

            public abstract void fire();
        }

        interface Listener extends EventListener {

            void fire();
        }
    }
}
