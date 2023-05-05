package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.internal.EventBusImpl;
import net.covers1624.eventbus.util.mock.MockEnvironment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by covers1624 on 5/5/23.
 */
public class TestFastInvokeOnly {

    static {
        System.setProperty("net.covers1624.eventbus.debug", "true");
    }

    public static final EventBus BUS = new EventBusImpl(MockEnvironment.WITH_CLASSES);

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

        interface Listener extends EventListener<SomeEvent> {

            void fire();
        }
    }
}
