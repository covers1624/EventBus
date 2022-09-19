package net.covers1624.eventbus.util;

import net.covers1624.eventbus.api.Environment;
import net.covers1624.eventbus.api.Event;
import net.covers1624.eventbus.util.ClassGenerator.GeneratedField;
import net.covers1624.quack.collection.StreamableIterable;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static net.covers1624.eventbus.util.Utils.asmName;
import static net.covers1624.eventbus.util.Utils.debugWriteClass;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.VOID_TYPE;

/**
 * Created by covers1624 on 11/4/21.
 */
public class EventClassGenerator {

    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    private static final AtomicInteger COUNTER = new AtomicInteger();

    // TODO use a Guava cache with access expiry.
    private final Map<Class<? extends Event>, Class<? extends Event>> eventClassCache = new ConcurrentHashMap<>();

    public Class<? extends Event> createEventClass(Environment env, Class<? extends Event> eventInterface) {
        synchronized (eventInterface) {
            return eventClassCache.computeIfAbsent(eventInterface, e -> generateClass(env, e));
        }
    }

    private Class<? extends Event> generateClass(Environment env, Class<? extends Event> clazz) {
        Map<String, EventField> fields = EventFieldExtractor.getEventFields(clazz);

        ClassGenerator classGen = new ClassGenerator(
                ACC_PUBLIC | ACC_SUPER | ACC_FINAL | ACC_SYNTHETIC,
                Type.getObjectType(asmName(clazz.getName()) + "$$Impl$$" + COUNTER.getAndIncrement())
        );
        classGen.withInterface(Type.getType(clazz));

        List<GeneratedField> genFields = new ArrayList<>(fields.size());
        for (EventField field : fields.values()) {
            GeneratedField genField = classGen.addField(field.isImmutable() ? ACC_PRIVATE | ACC_FINAL : ACC_PRIVATE, field.name, field.getType());
            genFields.add(genField);

            classGen.addMethod(ACC_PUBLIC | ACC_FINAL, field.getter, gen -> {
                gen.loadThis();
                gen.getField(genField);
                gen.ret();
            });

            if (field.setter != null) {
                classGen.addMethod(ACC_PUBLIC | ACC_FINAL, field.setter, gen -> {
                    gen.loadThis();
                    gen.loadParam(0);
                    gen.putField(genField);
                    gen.ret();
                });
            }
        }

        // Constructor with all fields as parameters.
        Type[] ctorArgs = StreamableIterable.of(fields.values()).map(EventField::getType).toArray(new Type[0]);
        classGen.addMethod(ACC_PUBLIC, "<init>", Type.getMethodType(VOID_TYPE, ctorArgs), gen -> {
            gen.loadThis();
            gen.methodInsn(INVOKESPECIAL, OBJECT_TYPE, "<init>", Type.getMethodType(VOID_TYPE), false);
            for (int i = 0; i < ctorArgs.length; i++) {
                gen.loadThis();
                gen.loadParam(i);
                gen.putField(genFields.get(i));
            }
            gen.ret();
        });

        byte[] bytes = classGen.build();
        String cName = classGen.getName().getInternalName();
        if (Environment.DEBUG) {
            debugWriteClass(cName, bytes);
        }

        //noinspection unchecked
        return (Class<? extends Event>) env.getClassDefiner().defineClass(cName.replace("/", "."), bytes);
    }
}
