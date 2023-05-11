package net.covers1624.eventbus.internal;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.api.EventFactory;
import net.covers1624.quack.asm.ClassBuilder;
import net.covers1624.quack.asm.ClassBuilder.FieldBuilder;
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

    private static final Type REGISTERED_EVENT = Type.getType(EventListenerList.class);
    private static final Method M_REBUILD = SneakyUtils.sneaky(() -> EventListenerList.class.getDeclaredMethod("rebuildEventList"));

    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    /**
     * Generates an implementation of the user provided {@link EventFactory} with {@link EventFactoryInternal} stapled on top.
     * <p>
     * The goal of this is to have a fast bridge between the generated event list and the root factory.
     *
     * <pre>
     * We do the following:
     * - Generate a synthetic class extending user specified {@link EventFactory} class, implementing {@link EventFactoryInternal}.
     * - Generate a constructor taking the {@link EventListenerList} instance.
     * - Generate a synthetic field to store the generated factory instance.
     * - Generate a synthetic field to mark the factory as dirty.
     * - Generate an implementation of {@link EventFactoryInternal#setDirty()} and {@link EventFactoryInternal#isDirty()} which
     *   set and check the dirty flag respectively.
     * - Generate an implementation of {@link EventFactoryInternal#setFactory} which sets the internal factory field
     *   as well as clearing the dirty flag.
     * - An implementation of the provided {@link EventFactory}'s sole abstract method which:
     *   - Checks the dirty flag
     *   - If the dirty flag is set, regenerates the event factory.
     *   - Forwards through to the factory field's sole abstract method.
     * </pre>
     *
     * @param event The event we are generating a {@link EventFactory} for.
     * @return The generated event factory.
     */
    // TODO cache these?
    public static EventFactory generate(EventListenerList event) {
        Class<?> factory = event.eventFactory;
        Type factoryType = Type.getType(factory);
        Method forwardMethod = getSingleAbstractMethod(factory);

        // TODO see how these names get generated for inner classes.
        ClassBuilder classGen = new ClassBuilder(ACC_PUBLIC | ACC_SUPER | ACC_FINAL | ACC_SYNTHETIC, synClassName(EventFactoryInternal.class, "Decorated", factory, COUNTER))
                .withParent(factoryType)
                .withInterface(Type.getType(EventFactoryInternal.class));

        FieldBuilder eventField = classGen.addField(ACC_PRIVATE | ACC_FINAL, "event", REGISTERED_EVENT);
        FieldBuilder factoryField = classGen.addField(ACC_PRIVATE, "factory", factoryType);
        FieldBuilder dirtyField = classGen.addField(ACC_PRIVATE, "dirty", BOOLEAN_TYPE);

        Type ctorDesc = Type.getMethodType(VOID_TYPE, REGISTERED_EVENT);
        classGen.addMethod(ACC_PUBLIC, "<init>", ctorDesc).withBody(gen -> {
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

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, "setFactory", Type.getMethodType(VOID_TYPE, OBJECT_TYPE)).withBody( gen -> {
            gen.loadThis();
            gen.loadParam(0);
            gen.typeInsn(CHECKCAST, factoryType);
            gen.putField(factoryField);
            gen.loadThis();
            gen.ldcInt(0);
            gen.putField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, "setDirty", Type.getMethodType(VOID_TYPE)).withBody( gen -> {
            gen.loadThis();
            gen.ldcInt(1);
            gen.putField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, "isDirty", Type.getMethodType(BOOLEAN_TYPE)).withBody( gen -> {
            gen.loadThis();
            gen.getField(dirtyField);
            gen.ret();
        });

        classGen.addMethod(ACC_PUBLIC | ACC_FINAL, forwardMethod.getName(), Type.getType(forwardMethod)).withBody( gen -> {
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
        String cName = classGen.name().getInternalName();
        if (Environment.DEBUG) {
            debugWriteClass(cName, bytes);
        }

        Class<?> c = event.bus.environment.defineClass(cName.replace("/", "."), bytes);
        try {
            return (EventFactory) c.getConstructor(EventListenerList.class).newInstance(event);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
