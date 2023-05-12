package net.covers1624.eventbus.test;

import net.covers1624.eventbus.api.*;
import net.covers1624.eventbus.internal.EventBusImpl;
import org.junit.jupiter.api.Test;

/**
 * Created by covers1624 on 18/9/22.
 */
public class TestClass extends TestBase {

    public static final EventBus BUS = new EventBusImpl(WITH_RESOURCES);

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

        BUS.constructFactory(SomeEvent.Factory.class, SomeEvent.class).fire("A", 1, 33.2);
    }

    @SubscribeEvent (SomeEvent.class)
    public void onSomeEvent(String string) {
        System.out.println(string);
    }

    @SubscribeEvent
    public void onSomeEventClass(SomeEvent event) {
        System.out.println(event.getString());
    }

    public interface SomeEvent extends Event {

//        Factory FACTORY = BUS.constructFactory(Factory.class, SomeEvent.class);

        String getString();

        int getInt();

        double getDouble();

        void setInt(int i);

        abstract class Factory extends EventFactory {

            // @Deprecated, use bellow, blah
            public void fire(@Named ("string") String string, @Named ("double") double d) {
                fire(string, 1, d);
            }

            public abstract void fire(@Named ("string") String string, @Named ("int") int i, @Named ("double") double d);
        }

        // @Deprecated, use bellow, blah
        interface Listener extends EventListener {

            void fire(@Named ("string") String string, @Named ("double") double d);
        }

        interface ListenerV2 extends EventListener {

            void fire(@Named ("string") String string, @Named ("int") int i, @Named ("double") double d);
        }
    }

}
