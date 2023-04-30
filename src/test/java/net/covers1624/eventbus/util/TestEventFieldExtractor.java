package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.CancelableEvent;
import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.api.GenericEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 10/4/21.
 */
public class TestEventFieldExtractor {

    @Test
    @Disabled ("Cancellable events need some work.")
    public void testCancelableEvent() {
        Map<String, EventField> fields = EventFieldExtractor.getEventFields(CancelableEvent.class);
        assertEquals(1, fields.size());
        EventField field = fields.get("canceled");
        assertNotNull(field);
        assertEquals(field.name, "canceled");
        assertNotNull(field.getter);
        assertNotNull(field.setter);
    }

    @Test
    public void testMultipleHierarchy() {
        Map<String, EventField> fields = EventFieldExtractor.getEventFields(Hierarchy.class);
        assertEquals(4, fields.size());
        assertNotNull(fields.get("hierarchy"));
        assertNotNull(fields.get("hierarchy1"));
        assertNotNull(fields.get("hierarchy2"));
        assertNotNull(fields.get("hierarchy3"));
    }

    @Test
    public void testGenerics() {
        Map<String, EventField> fields = EventFieldExtractor.getEventFields(Generics.class);
        assertNotNull(fields.get("object")); // TODO test this more
    }

    @Test
    public void testDuplicate() {
        Exception e = assertThrows(IllegalStateException.class, () -> EventFieldExtractor.getEventFields(Duplicate.class));
        assertEquals("Found duplicate Event field name 'hierarchy1'. Declared in 'net.covers1624.eventbus.util.TestEventFieldExtractor$Hierarchy1' and 'net.covers1624.eventbus.util.TestEventFieldExtractor$DuplicateH1'.", e.getMessage());
    }

    @Test
    public void testDifferentTypes() {
        Exception e = assertThrows(IllegalStateException.class, () -> EventFieldExtractor.getEventFields(DifferentTypes.class));
        assertEquals("Unexpected type. Got: class java.lang.String, Expected: interface java.lang.CharSequence", e.getMessage());
    }

    @Test
    public void testNoGetter() {
        Exception e = assertThrows(IllegalStateException.class, () -> EventFieldExtractor.getEventFields(NoGetter.class));
        assertEquals("Getter for event field 'field' in class 'net.covers1624.eventbus.util.TestEventFieldExtractor$NoGetter' required.", e.getMessage());
    }

    @Test
    public void testDoesntExtendEvent() {
        assertTrue(EventFieldExtractor.getEventFields(DoesntExtendEvent.class).isEmpty());
    }

    @Test
    public void testClassNotInterface() {
        Exception e = assertThrows(IllegalStateException.class, () -> EventFieldExtractor.getEventFields(ClassNotInterface.class));
        assertEquals("Expected interface. Got: class net.covers1624.eventbus.util.TestEventFieldExtractor$ClassNotInterface", e.getMessage());
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

        String getHierarchy();

        void setHierarchy(String s);
    }

    public interface Generics<T> extends GenericEvent<T> {

        T getObject();
    }

    public interface DuplicateH1 extends Event {

        String getHierarchy1();

    }

    public interface Duplicate extends DuplicateH1, Hierarchy1 {

    }

    public interface DifferentTypes extends Event {

        String getField();

        void setField(CharSequence s);
    }

    public interface NoGetter extends Event {

        void setField(String field);
    }

    public interface DoesntExtendEvent {

        String getField();
    }

    public static class ClassNotInterface implements Event {

        public String getField() {
            return "qwertyuiop";
        }
    }

}
