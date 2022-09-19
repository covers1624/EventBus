package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.util.mock.MockEnvironment;
import org.junit.jupiter.api.Test;

/**
 * Created by covers1624 on 11/4/21.
 */
public class TestEventClassGenerator {

    static {
        System.setProperty("net.covers1624.eventbus.debug", "true");
    }

    @Test
    public void doTest() {
        EventClassGenerator generator = new EventClassGenerator();
        generator.createEventClass(MockEnvironment.WITH_CLASSES, Hierarchy.class);
    }

    public interface Hierarchy1 extends Event {

        String getHierarchy1();

        void setHierarchy1(String s);
    }

    public interface Hierarchy2 extends Hierarchy1 {

        String getHierarchy2();

        void setHierarchy2(String s);
    }

    public interface Hierarchy3 extends Event {

        String getHierarchy3();

        void setHierarchy3(String s);
    }

    public interface Hierarchy extends Event, Hierarchy1, Hierarchy2, Hierarchy3 {

        String getImmutableField();

        String getHierarchy();

        void setHierarchy(String s);
    }
}
