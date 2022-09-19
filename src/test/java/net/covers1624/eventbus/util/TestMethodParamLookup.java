package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.Named;
import net.covers1624.eventbus.util.mock.MockEnvironment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 16/9/22.
 */
public class TestMethodParamLookup {

    static {
        System.setProperty("net.covers1624.eventbus.debug", "true");
    }

    @Test
    public void testMethodsWithoutASM() throws Throwable {
        Class<?> clazz = TestClass.class;
        MethodParamLookup lookup = new MethodParamLookup(MockEnvironment.WITHOUT_CLASSES);

        List<String> onEventNames = lookup.findParameterNames(clazz.getMethod("onEvent", String.class, String.class, List.class));
        assertEquals(0, onEventNames.size());

        List<String> onEventNamedNames = lookup.findParameterNames(clazz.getMethod("onEventNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    @Test
    public void testMethodsWithASM() throws Throwable {
        Class<?> clazz = TestClass.class;
        MethodParamLookup lookup = new MethodParamLookup(MockEnvironment.WITH_CLASSES);

        List<String> onEventNames = lookup.findParameterNames(clazz.getMethod("onEvent", String.class, String.class, List.class));
        assertEquals(3, onEventNames.size());
        assertEquals("a", onEventNames.get(0));
        assertEquals("b", onEventNames.get(1));
        assertEquals("c", onEventNames.get(2));

        List<String> onEventNamedNames = lookup.findParameterNames(clazz.getMethod("onEventNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    @Test
    public void testInterfaceWithoutASM() throws Throwable {
        Class<?> clazz = TestInterface.class;
        MethodParamLookup lookup = new MethodParamLookup(MockEnvironment.WITHOUT_CLASSES);

        List<String> onEventNames = lookup.findParameterNames(clazz.getMethod("fire", String.class, String.class, List.class));
        assertEquals(0, onEventNames.size());

        List<String> onEventNamedNames = lookup.findParameterNames(clazz.getMethod("fireNamed", String.class, String.class, List.class));
        assertEquals(3, onEventNamedNames.size());
        assertEquals("one", onEventNamedNames.get(0));
        assertEquals("two", onEventNamedNames.get(1));
        assertEquals("three", onEventNamedNames.get(2));
    }

    @Test
    public void testInterfaceWithASM() throws Throwable {
        Class<?> clazz = TestInterface.class;
        MethodParamLookup lookup = new MethodParamLookup(MockEnvironment.WITH_CLASSES);

        List<String> onEventNames = lookup.findParameterNames(clazz.getMethod("fire", String.class, String.class, List.class));
        assertEquals(0, onEventNames.size());

        List<String> onEventNamedNames = lookup.findParameterNames(clazz.getMethod("fireNamed", String.class, String.class, List.class));
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
