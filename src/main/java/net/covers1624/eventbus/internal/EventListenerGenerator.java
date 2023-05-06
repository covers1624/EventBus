package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.util.*;
import net.covers1624.eventbus.util.ClassGenerator.GeneratedField;
import net.covers1624.eventbus.util.ClassGenerator.InsnGenerator.Var;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.covers1624.eventbus.util.Utils.asmName;
import static net.covers1624.eventbus.util.Utils.debugWriteClass;
import static org.objectweb.asm.Opcodes.*;

/**
 * Created by covers1624 on 18/9/22.
 */
public class EventListenerGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final EventClassGenerator EVENT_CLASS_GENERATOR = new EventClassGenerator();

    public static Object generateEventFactory(EventListenerList event) {
        Class<?> eventFactory = event.eventFactory;
        List<ListenerHandle> listeners = event.getListeners();
        Method factoryMethod = Utils.getSingleAbstractMethod(eventFactory);
        List<String> factoryParams = event.bus.paramLookup.findParameterNames(factoryMethod);

        ClassGenerator classGen = new ClassGenerator(
                ACC_PUBLIC | ACC_SUPER | ACC_FINAL | ACC_SYNTHETIC,
                Type.getObjectType(asmName(eventFactory) + "$$Impl$$" + COUNTER.getAndIncrement())
        );
        classGen.withParent(Type.getType(eventFactory));

        boolean requiresEventClass = ColUtils.anyMatch(listeners, e -> !e.isFastInvoke());
        Map<String, EventField> eventFields = EventFieldExtractor.getEventFields(event.eventInterface);

        AtomicInteger instanceFieldCounter = new AtomicInteger();
        Map<ListenerHandle, GeneratedField> instanceFields = new LinkedHashMap<>();
        for (ListenerHandle listener : listeners) {
            if (listener.instance != null) {
                GeneratedField instanceField = classGen.addField(
                        ACC_PRIVATE | ACC_FINAL,
                        "instance$" + instanceFieldCounter.getAndIncrement(),
                        Type.getType(listener.handle.getDeclaringClass())
                );
                instanceFields.put(listener, instanceField);
            }
        }

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, factoryMethod, gen -> {
            Map<String, Var> fieldVars = new HashMap<>();
            for (int i = 0; i < factoryParams.size(); i++) {
                fieldVars.put(factoryParams.get(i), gen.param(i));
            }

            Map<String, Method> eventGetters = new HashMap<>();
            Var eventVar = null;
            if (requiresEventClass) {
                Class<? extends Event> eventClass = EVENT_CLASS_GENERATOR.createEventClass(event.bus.environment, event.eventInterface);
                Constructor<?> ctor = eventClass.getConstructors()[0];
                Map<String, Method> methods = FastStream.of(eventClass.getDeclaredMethods())
                        .toImmutableMap(Method::getName, e -> e);

                for (EventField value : eventFields.values()) {
                    eventGetters.put(value.name, methods.get(value.getter.getName()));
                }

                eventVar = gen.newVar(Type.getType(eventClass));
                gen.typeInsn(NEW, Type.getType(eventClass));
                gen.insn(DUP);
                for (String s : eventFields.keySet()) {
                    gen.load(fieldVars.get(s));
                }
                gen.methodInsn(INVOKESPECIAL, Type.getType(eventClass), "<init>", Type.getType(ctor), false);
                gen.store(eventVar);
            }

            for (ListenerHandle listener : listeners) {
                if (listener.instance != null) {
                    // Emit load of instance field for handle invoke.
                    gen.loadThis();
                    gen.getField(instanceFields.get(listener));
                }
                // Emit all parameters for handle invoke.
                if (listener.isFastInvoke()) {
                    // Emit each parameter as declared by the handle.
                    for (String param : listener.getParams()) {
                        EventField field = eventFields.get(param);
                        if (field.isImmutable() || eventVar == null) {
                            // Field is immutable, we can just emit the param.
                            gen.load(fieldVars.get(param));
                        } else {
                            // Field is not immutable, emit virtual call to the event class getter. (avoid invokeinterface)
                            gen.load(eventVar);
                            gen.methodInsn(INVOKEVIRTUAL, eventGetters.get(param));
                        }
                    }
                } else {
                    // Emit just a load of the event class
                    gen.load(eventVar);
                }
                // Emit invoke of handle
                gen.methodInsn(listener.handle);
                // TODO assert handle has void return and add pops if required.
            }
            gen.ret();
        });

        Type[] ctorArgs = FastStream.of(instanceFields.values())
                .map(e -> e.desc)
                .toArray(new Type[0]);
        classGen.addMethod(ACC_PUBLIC, "<init>", Type.getMethodType(Type.VOID_TYPE, ctorArgs), gen -> {
            gen.loadThis();
            gen.methodInsn(INVOKESPECIAL, Type.getType(eventFactory), "<init>", Type.getMethodType(Type.VOID_TYPE), false);
            int i = 0;
            for (GeneratedField value : instanceFields.values()) {
                gen.loadThis();
                gen.loadParam(i++);
                gen.putField(value);
            }
            gen.ret();
        });

        byte[] bytes = classGen.build();
        String cName = classGen.getName().getInternalName();
        if (Environment.DEBUG) {
            debugWriteClass(cName, bytes);
        }
        Class<?> clazz = event.bus.environment.defineClass(cName.replace("/", "."), bytes);
        Constructor<?> ctor = clazz.getConstructors()[0];

        try {
            return ctor.newInstance(FastStream.of(instanceFields.keySet()).map(e -> e.instance).toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
