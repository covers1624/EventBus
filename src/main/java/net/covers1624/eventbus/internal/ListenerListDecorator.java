package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.api.EventInvoker;
import net.covers1624.eventbus.util.ClassGenerator;
import net.covers1624.eventbus.util.ClassGenerator.GeneratedField;
import net.covers1624.quack.util.SneakyUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static net.covers1624.eventbus.util.Utils.debugWriteClass;
import static net.covers1624.eventbus.util.Utils.getSingleMethod;
import static org.objectweb.asm.Opcodes.*;

/**
 * Created by covers1624 on 17/9/22.
 */
public class ListenerListDecorator {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final Type EVENT_LISTENER_LIST = Type.getType(EventListenerList.class);
    private static final Field F_INVALID = SneakyUtils.sneaky(() -> EventListenerList.class.getDeclaredField("invalid"));
    private static final Method M_GENERATE_LIST = SneakyUtils.sneaky(() -> EventListenerList.class.getDeclaredMethod("generateList"));

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type CLASS_TYPE = Type.getType(Class.class);

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
     * @param eventClass   The event class we are generating a decorated {@link EventListenerList} for.
     * @param invokerClass The event's {@link EventInvoker} interface.
     * @return The newly constructed {@link EventListenerList} class.
     */
    // TODO make this Return Class<? extends EventInvoker>
    // TODO cache these?
    public static <T extends EventInvoker> T generate(RegisteredEvent event) {
        Method forwardMethod = getSingleMethod(event.eventInvoker);
        Type invokerType = Type.getType(event.eventInvoker);

        // TODO see how these names get generated for inner classes.
        ClassGenerator classGen = new ClassGenerator(ACC_PUBLIC | ACC_SUPER | ACC_FINAL | ACC_SYNTHETIC, Type.getObjectType(EVENT_LISTENER_LIST.getInternalName() + "$$Decorated$$" + event.eventInterface.getSimpleName() + "$$" + COUNTER.getAndIncrement()))
                .withParent(EVENT_LISTENER_LIST)
                .withInterface(invokerType);

        GeneratedField invokerField = classGen.addField(ACC_PRIVATE, "invoker", invokerType);

        Type ctorDesc = Type.getMethodType(Type.VOID_TYPE, Type.getType(RegisteredEvent.class));
        classGen.addMethod(ACC_PUBLIC, "<init>", ctorDesc, gen -> {
            gen.loadThis();
            gen.loadParam(0);
            gen.methodInsn(INVOKESPECIAL, EVENT_LISTENER_LIST, "<init>", ctorDesc, false);
            gen.ret();
        });

        classGen.addMethod(ACC_PROTECTED | ACC_FINAL, "setInvoker", Type.getMethodType(Type.VOID_TYPE, OBJECT_TYPE), gen -> {
            gen.loadThis();
            gen.loadParam(0);
            gen.typeInsn(CHECKCAST, invokerType);
            gen.putField(invokerField);
            gen.ret();
        });

        classGen.addMethod(ACC_PROTECTED | ACC_FINAL, "getInvoker", Type.getMethodType(OBJECT_TYPE), gen -> {
            gen.loadThis();
            gen.getField(invokerField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, forwardMethod.getName(), Type.getType(forwardMethod), gen -> {
            Label fire = new Label();
            Label compute = new Label();
            gen.loadThis();
            gen.getField(invokerField);
            gen.insn(DUP);

            gen.jump(IFNULL, compute);
            gen.loadThis();
            gen.getField(F_INVALID);
            gen.jump(IFNE, compute);
            gen.jump(GOTO, fire);

            gen.label(compute);
            gen.insn(POP);
            gen.loadThis();
            gen.methodInsn(INVOKEVIRTUAL, M_GENERATE_LIST);
            gen.loadThis();
            gen.getField(invokerField);

            gen.label(fire);
            for (int i = 0; i < gen.numParams(); i++) {
                gen.loadParam(i);
            }
            gen.methodInsn(INVOKEINTERFACE, forwardMethod);
            gen.ret();
        });

        byte[] bytes = classGen.build();
        String cName = classGen.getName().getInternalName();
        if (Environment.DEBUG) {
            debugWriteClass(cName, bytes);
        }

        Class<?> c = event.bus.environment.getClassDefiner().defineClass(cName.replace("/", "."), bytes);
        try {
            //noinspection unchecked
            return (T) c.getConstructor(RegisteredEvent.class).newInstance(event);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
