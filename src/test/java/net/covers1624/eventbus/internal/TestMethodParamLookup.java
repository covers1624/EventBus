package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Named;
import net.covers1624.eventbus.test.TestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 16/9/22.
 */
public class TestMethodParamLookup extends TestBase {

    @Test
    public void testMethodsWithoutASM() throws Throwable {
        Class<?> clazz = TestClass.class;
        MethodParamLookup lookup = new MethodParamLookup(WITHOUT_RESOURCES);

        List<String> onEventNames = lookup.getMethodParams(clazz.getMethod("onEvent", String.class, String.class, List.class));
        assertEquals(0, onEventNames.size());

        List<String> onEventNamedNames = lookup.getMethodParams(clazz.getMethod("onEventNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    @Test
    public void testMethodsWithASM() throws Throwable {
        Class<?> clazz = TestClass.class;
        MethodParamLookup lookup = new MethodParamLookup(WITH_RESOURCES);

        List<String> onEventNames = lookup.getMethodParams(clazz.getMethod("onEvent", String.class, String.class, List.class));
        assertEquals(3, onEventNames.size());
        assertEquals("a", onEventNames.get(0));
        assertEquals("b", onEventNames.get(1));
        assertEquals("c", onEventNames.get(2));

        List<String> onEventNamedNames = lookup.getMethodParams(clazz.getMethod("onEventNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    @Test
    public void testInterfaceWithoutASM() throws Throwable {
        Class<?> clazz = TestInterface.class;
        MethodParamLookup lookup = new MethodParamLookup(WITHOUT_RESOURCES);

        List<String> onEventNames = lookup.getMethodParams(clazz.getMethod("fire", String.class, String.class, List.class));
        assertEquals(0, onEventNames.size());

        List<String> onEventNamedNames = lookup.getMethodParams(clazz.getMethod("fireNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    @Test
    public void testInterfaceWithASM() throws Throwable {
        Class<?> clazz = TestInterface.class;
        MethodParamLookup lookup = new MethodParamLookup(WITH_RESOURCES);

        List<String> onEventNames = lookup.getMethodParams(clazz.getMethod("fire", String.class, String.class, List.class));
        assertEquals(0, onEventNames.size());

        List<String> onEventNamedNames = lookup.getMethodParams(clazz.getMethod("fireNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    public static class TestClass {

        public void onEvent(String a, String b, List<String> c) {
        }

        public void onEventNamed(@Named ("one") String a, @Named ("two") String b, @Named ("three") List<String> c) {
        }
    }

    public interface TestInterface {

        void fire(String a, String b, List<String> c);

        void fireNamed(@Named ("one") String a, @Named ("two") String b, @Named ("three") List<String> c);
    }
}
