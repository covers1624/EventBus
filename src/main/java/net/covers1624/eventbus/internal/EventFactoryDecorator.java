package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.api.EventFactory;
import net.covers1624.eventbus.util.ClassGenerator;
import net.covers1624.eventbus.util.ClassGenerator.GeneratedField;
import net.covers1624.quack.util.SneakyUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static net.covers1624.eventbus.util.Utils.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;

/**
 * Created by covers1624 on 17/9/22.
 */
public class EventFactoryDecorator {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final Type REGISTERED_EVENT = Type.getType(RegisteredEvent.class);
    private static final Method M_REBUILD = SneakyUtils.sneaky(() -> RegisteredEvent.class.getDeclaredMethod("rebuildEventList"));

    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    /**
     * Generates an ASM Decorated class of {@link EventListenerList} implementing the provided invoker interface. The goal of this
     * is to have a super-fast bridge between the generated event list invoker, the root invoker and the caller.
     * <pre>
     * We do the following:
     * - Generate a synthetic class extending {@link EventListenerList} implementing the invoker interface.
     * - Generate a passthrough constructor.
     * - Generate a synthetic field to store the generated event list invoker instance called {@code invoker}.
     * - Implement a protected getter and setter for accessing the invoker field. Used by {@link EventListenerList} internally.
     * - Implement the only method provided on the invoker class, forwarding the call to the {@code invoker} instance.
     * </pre>
     * <p>
     * Control for generating the listener list invoker is handed off to {@link EventListenerList} to facilitate parallel dispatch of events, and potentially
     * parallel computation of the generated listener invoker.
     *
     * @return The newly constructed {@link EventListenerList} class.
     */
    // TODO cache these?
    public static EventFactory<?> generate(RegisteredEvent event) {
        Class<?> factory = event.getEventFactory();
        Type factoryType = Type.getType(factory);
        Method forwardMethod = getSingleAbstractMethod(factory);

        // TODO see how these names get generated for inner classes.
        ClassGenerator classGen = new ClassGenerator(ACC_PUBLIC | ACC_SUPER | ACC_FINAL | ACC_SYNTHETIC, synClassName(EventFactoryInternal.class, "Decorated", factory, COUNTER))
                .withParent(factoryType)
                .withInterface(Type.getType(EventFactoryInternal.class));

        GeneratedField eventField = classGen.addField(ACC_PRIVATE | ACC_FINAL, "event", REGISTERED_EVENT);
        GeneratedField factoryField = classGen.addField(ACC_PRIVATE, "factory", factoryType);
        GeneratedField dirtyField = classGen.addField(ACC_PRIVATE, "dirty", BOOLEAN_TYPE);

        Type ctorDesc = Type.getMethodType(VOID_TYPE, REGISTERED_EVENT);
        classGen.addMethod(ACC_PUBLIC, "<init>", ctorDesc, gen -> {
            gen.loadThis();
            gen.methodInsn(INVOKESPECIAL, factoryType, "<init>", Type.getMethodType(VOID_TYPE), false);
            gen.loadThis();
            gen.loadParam(0);
            gen.putField(eventField);
            gen.loadThis();
            gen.ldcInt(1);
            gen.putField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, "setFactory", Type.getMethodType(VOID_TYPE, OBJECT_TYPE), gen -> {
            gen.loadThis();
            gen.loadParam(0);
            gen.typeInsn(CHECKCAST, factoryType);
            gen.putField(factoryField);
            gen.loadThis();
            gen.ldcInt(0);
            gen.putField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, "setDirty", Type.getMethodType(VOID_TYPE), gen -> {
            gen.loadThis();
            gen.ldcInt(1);
            gen.putField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, "isDirty", Type.getMethodType(BOOLEAN_TYPE), gen -> {
            gen.loadThis();
            gen.getField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, forwardMethod.getName(), Type.getType(forwardMethod), gen -> {
            Label fire = new Label();
            Label compute = new Label();

            gen.loadThis();
            gen.getField(dirtyField);
            gen.jump(IFNE, compute);
            gen.jump(GOTO, fire);

            gen.label(compute);
            gen.loadThis();
            gen.getField(eventField);
            gen.methodInsn(INVOKEVIRTUAL, M_REBUILD);

            gen.label(fire);
            gen.loadThis();
            gen.getField(factoryField);
            for (int i = 0; i < gen.numParams(); i++) {
                gen.loadParam(i);
            }
            gen.methodInsn(INVOKEVIRTUAL, forwardMethod);
            gen.ret();
        });

        byte[] bytes = classGen.build();
        String cName = classGen.getName().getInternalName();
        if (Environment.DEBUG) {
            debugWriteClass(cName, bytes);
        }

        Class<?> c = event.bus.environment.getClassDefiner().defineClass(cName.replace("/", "."), bytes);
        try {
            return (EventFactory<?>) c.getConstructor(RegisteredEvent.class).newInstance(event);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
