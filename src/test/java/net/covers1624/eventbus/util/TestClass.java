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
    public static final SomeEvent.Invoker INVOKER = BUS.registerEvent(SomeEvent.Invoker.class, SomeEvent.class);

    @Test
    public void doTest() {
        Class<?> CLASS = SomeEvent.class; // HACK
        BUS.registerListener(SomeEvent.Invoker.class, (str, i, d) -> {
            System.out.println(str + " " + i + " " + d);
        });
//        BUS.registerListener(SomeEvent.class, e -> {
//            System.out.println(e);
//        });
        BUS.register(this);

        INVOKER.fire("A", 1, 33.2);
    }

    @SubscribeEvent (SomeEvent.class)
    public void onSomeEvent(String string) {
        System.out.println(string);
    }


    public interface SomeEvent extends Event {

        String getString();

        int getInt();

        double getDouble();

        void setInt(int i);

        interface Invoker extends EventInvoker {

            void fire(@Named ("string") String string, @Named ("int") int i, @Named ("double") double d);
        }
    }

}
