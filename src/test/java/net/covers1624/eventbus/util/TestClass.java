package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.internal.EventBusImpl;
import net.covers1624.eventbus.util.mock.MockEnvironment;
import org.junit.jupiter.api.Test;

/**
 * Created by covers1624 on 18/9/22.
 */
public class TestClass {

    static {
        System.setProperty("net.covers1624.eventbus.debug", "true");
    }

    public static final EventBus BUS = new EventBusImpl(MockEnvironment.WITH_CLASSES);

    @Test
    public void doTest() {
        System.out.println();
        BUS.registerListener(SomeEvent.ListenerV2.class, (str, i, d) -> {
            System.out.println(str + " " + i + " " + d);
        });
        BUS.registerListener(SomeEvent.Listener.class, (str, d) -> {
            System.out.println(str + " " + " " + d);
        });
        BUS.registerListener(SomeEvent.class, e -> {
            System.out.println(e);
        });
        BUS.register(this);

        SomeEvent.FACTORY.fire("A", 1, 33.2);
    }

    @SubscribeEvent (SomeEvent.class)
    public void onSomeEvent(String string) {
        System.out.println(string);
    }

    public interface SomeEvent extends Event {

        Factory FACTORY = BUS.registerEvent(Factory.class, SomeEvent.class);

        String getString();

        int getInt();

        double getDouble();

        void setInt(int i);

        abstract class Factory extends EventFactory<Factory> {

            // @Deprecated, use bellow, blah
            public void fire(@Named ("string") String string, @Named ("double") double d) {
                fire(string, 1, d);
            }

            public abstract void fire(@Named ("string") String string, @Named ("int") int i, @Named ("double") double d);
        }

        // @Deprecated, use bellow, blah
        interface Listener extends EventListener<SomeEvent> {

            void fire(@Named ("string") String string, @Named ("double") double d);
        }

        interface ListenerV2 extends EventListener<SomeEvent> {

            void fire(@Named ("string") String string, @Named ("int") int i, @Named ("double") double d);
        }
    }

}
