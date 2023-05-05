package net.covers1624.eventbus.util;

import net.covers1624.quack.collection.FastStream;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

/**
 * A very simple simi builder-like class for generating classes using Objectweb ASM.
 * <p>
 * Created by covers1624 on 17/9/22.
 */
public class ClassGenerator {

    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    private int classVersion = V1_8;
    private final int access;
    private final Type name;
    private Type parent = OBJECT_TYPE;
    private final List<Type> interfaces = new LinkedList<>();

    private final List<GeneratedField> fields = new LinkedList<>();
    private final List<GeneratedMethod> methods = new LinkedList<>();

    public ClassGenerator(int access, Type name) {
        this.access = access;
        this.name = name;
    }

    public Type getName() {
        return name;
    }

    public ClassGenerator classVersion(int classVersion) {
        this.classVersion = classVersion;
        return this;
    }

    public ClassGenerator withParent(Type parent) {
        this.parent = parent;
        return this;
    }

    public ClassGenerator withInterface(Type iFace) {
        interfaces.add(iFace);
        return this;
    }

    public GeneratedField addField(int access, String name, Type desc) {
        GeneratedField field = new GeneratedField(access, this, name, desc);
        fields.add(field);
        return field;
    }

    public GeneratedMethod addMethod(int access, Method method, Consumer<InsnGenerator> func) {
        return addMethod(access, method.getName(), Type.getType(method), func);
    }

    public GeneratedMethod addMethod(int access, String name, Type desc, Consumer<InsnGenerator> func) {
        GeneratedMethod method = new GeneratedMethod(access, this, name, desc, func);
        methods.add(method);
        return method;
    }

    public byte[] build() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String[] iFaces = FastStream.of(interfaces).map(Type::getInternalName).toArray(new String[0]);
        cw.visit(classVersion, access, name.getInternalName(), null, parent.getInternalName(), iFaces);

        for (GeneratedField field : fields) {
            field.write(cw);
        }

        for (GeneratedMethod method : methods) {
            method.write(cw);
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    public static class GeneratedField {

        public final int access;
        public final ClassGenerator owner;
        public final String name;
        public final Type desc;

        public GeneratedField(int access, ClassGenerator owner, String name, Type desc) {
            this.access = access;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        private void write(ClassVisitor cv) {
            FieldVisitor fv = cv.visitField(access, name, desc.getDescriptor(), null, null);
            fv.visitEnd();
        }
    }

    public static class GeneratedMethod {

        public final int access;
        public final ClassGenerator owner;
        public final String name;
        public final Type desc;
        @Nullable
        private final Consumer<InsnGenerator> generator;

        public GeneratedMethod(int access, ClassGenerator owner, String name, Type desc, @Nullable Consumer<InsnGenerator> generator) {
            this.access = access;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.generator = generator;
        }

        private void write(ClassVisitor cv) {
            MethodVisitor mv = cv.visitMethod(access, name, desc.getDescriptor(), null, null);
            if (generator != null) {
                InsnGenerator gen = new InsnGenerator(this);
                generator.accept(gen);
                gen.write(mv);
            }
            mv.visitEnd();
        }
    }

    public static class InsnGenerator {

        private final GeneratedMethod method;
        private final InsnList insns = new InsnList();
        private final List<TryCatchBlockNode> tryCatches = new LinkedList<>();

        @Nullable
        private final Var thisVar;
        private final Var[] params;
        private final BitSet usedVars = new BitSet();

        public InsnGenerator(GeneratedMethod method) {
            this.method = method;
            thisVar = (method.access & ACC_STATIC) == 0 ? pushVar(method.owner.name, true) : null;
            Type[] types = method.desc.getArgumentTypes();
            params = new Var[types.length];
            for (int i = 0; i < types.length; i++) {
                params[i] = pushVar(types[i], true);
            }
        }

        private Var pushVar(Type type, boolean immutable) {
            int index = 0;
            while (true) {
                index = usedVars.nextClearBit(index);
                if (type.getSize() != 2) break;
                if (usedVars.nextClearBit(index + 1) - 1 == index) {
                    break;
                }
            }
            usedVars.set(index, index + type.getSize());
            return new Var(this, index, type, immutable);
        }

        private void popVar(Var var) {
            usedVars.clear(var.index, var.index + var.type.getSize());
        }

        public Var getThis() {
            if (thisVar == null) throw new UnsupportedOperationException("Static methods don't have 'this'.");

            return thisVar;
        }

        public int numParams() {
            return params.length;
        }

        public Var param(int index) {
            return params[index];
        }

        public Var newVar(Type type) {
            return pushVar(type, false);
        }

        public void ret() {
            insn(method.desc.getReturnType().getOpcode(IRETURN));
        }

        public void insn(int opcode) {
            insns.add(new InsnNode(opcode));
        }

        public void intInsn(int opcode, int operand) {
            insns.add(new IntInsnNode(opcode, operand));
        }

        public void loadThis() {
            load(getThis());
        }

        public void loadParam(int index) {
            load(param(index));
        }

        public void load(Var var) {
            varInsn(var.type.getOpcode(ILOAD), var.getIndex());
        }

        public void storeParam(int index) {
            store(param(index));
        }

        public void store(Var var) {
            varInsn(var.type.getOpcode(ISTORE), var.getIndex());
        }

        public void varInsn(int opcode, int var) {
            insns.add(new VarInsnNode(opcode, var));
        }

        public void typeInsn(int opcode, Type type) {
            assert type.getSort() == Type.OBJECT;

            insns.add(new TypeInsnNode(opcode, type.getInternalName()));
        }

        public void getField(Field field) {
            fieldInsn(Modifier.isStatic(field.getModifiers()) ? GETSTATIC : GETFIELD, field);
        }

        public void putField(Field field) {
            fieldInsn(Modifier.isStatic(field.getModifiers()) ? PUTSTATIC : PUTFIELD, field);
        }

        public void fieldInsn(int opcode, Field field) {
            fieldInsn(opcode, Type.getType(field.getDeclaringClass()), field.getName(), Type.getType(field.getType()));
        }

        public void getField(GeneratedField field) {
            fieldInsn((field.access & ACC_STATIC) != 0 ? GETSTATIC : GETFIELD, field);
        }

        public void putField(GeneratedField field) {
            fieldInsn((field.access & ACC_STATIC) != 0 ? PUTSTATIC : PUTFIELD, field);
        }

        public void fieldInsn(int opcode, GeneratedField field) {
            fieldInsn(opcode, field.owner.name, field.name, field.desc);
        }

        public void fieldInsn(int opcode, Type owner, String name, Type descriptor) {
            assert owner.getSort() == Type.OBJECT;

            insns.add(new FieldInsnNode(opcode, owner.getInternalName(), name, descriptor.getDescriptor()));
        }

        public void methodInsn(Method method) {
            int opcode;
            // TODO, ctor/super calls?
            if (Modifier.isStatic(method.getModifiers())) {
                opcode = INVOKESTATIC;
            } else if (Modifier.isInterface(method.getDeclaringClass().getModifiers())) {
                opcode = INVOKEINTERFACE;
            } else {
                opcode = INVOKEVIRTUAL;
            }
            methodInsn(opcode, method);
        }

        public void methodInsn(int opcode, Method method) {
            methodInsn(opcode, Type.getType(method.getDeclaringClass()), method.getName(), Type.getType(method), Modifier.isInterface(method.getDeclaringClass().getModifiers()));
        }

        public void methodInsn(int opcode, GeneratedMethod method) {
            methodInsn(opcode, method.owner.name, method.name, method.desc, (method.owner.access & ACC_INTERFACE) != 0);
        }

        public void methodInsn(int opcode, Type owner, String name, Type descriptor, boolean isInterface) {
            assert owner.getSort() == Type.OBJECT;

            insns.add(new MethodInsnNode(opcode, owner.getInternalName(), name, descriptor.getDescriptor(), isInterface));
        }

        public void invokeDynamic(String name, Type descriptor, Handle boostrapMethod, Object... bsmArgs) {
            insns.add(new InvokeDynamicInsnNode(name, descriptor.getDescriptor(), boostrapMethod, bsmArgs));
        }

        public void jump(int opcode, Label label) {
            insns.add(new JumpInsnNode(opcode, getLabelNode(label)));
        }

        public void label(Label label) {
            insns.add(getLabelNode(label));
        }

        public void ldcInt(int i) {
            ldc(i);
        }

        public void ldcFloat(float f) {
            ldc(f);
        }

        public void ldcLong(long l) {
            ldc(l);
        }

        public void ldcDouble(double d) {
            ldc(d);
        }

        public void ldcString(String str) {
            ldc(str);
        }

        public void ldcClass(Type type) {
            assert type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;

            ldc(type);
        }

        public void ldc(Object obj) {
            insns.add(new LdcInsnNode(obj));
        }

        public void iinc(int var, int increment) {
            insns.add(new IincInsnNode(var, increment));
        }

        public void tableSwitch(int min, int max, Label default_, Label... labels) {
            LabelNode[] labelNodes = FastStream.of(labels)
                    .map(this::getLabelNode)
                    .toArray(new LabelNode[0]);
            insns.add(new TableSwitchInsnNode(min, max, getLabelNode(default_), labelNodes));
        }

        public void lookupSwitch(Label default_, int[] keys, Label[] labels) {
            LabelNode[] labelNodes = FastStream.of(labels)
                    .map(this::getLabelNode)
                    .toArray(new LabelNode[0]);
            insns.add(new LookupSwitchInsnNode(getLabelNode(default_), keys, labelNodes));
        }

        public void multiNewArray(Type descriptor, int numDimensions) {
            insns.add(new MultiANewArrayInsnNode(descriptor.getDescriptor(), numDimensions));
        }

        public void tryCatchBlock(Label start, Label end, Label handler, @Nullable Type type) {
            tryCatches.add(new TryCatchBlockNode(getLabelNode(start), getLabelNode(end), getLabelNode(handler), type != null ? type.getDescriptor() : null));
        }

        private LabelNode getLabelNode(final Label label) {
            if (!(label.info instanceof LabelNode)) {
                label.info = new LabelNode();
            }
            return (LabelNode) label.info;
        }

        private void write(MethodVisitor mv) {
            mv.visitCode();
            for (TryCatchBlockNode tryCatch : tryCatches) {
                tryCatch.accept(mv);
            }
            insns.accept(mv);
            mv.visitMaxs(-1, -1);
        }

        public static class Var {

            private final InsnGenerator gen;
            private final int index;
            private final Type type;
            private final boolean immutable;
            private boolean freed;

            public Var(InsnGenerator gen, int index, Type type, boolean immutable) {
                this.gen = gen;
                this.index = index;
                this.type = type;
                this.immutable = immutable;
            }

            public int getIndex() {
                if (freed) throw new IllegalStateException("Use after free.");

                return index;
            }

            public void free() {
                if (immutable) throw new UnsupportedOperationException("Variable is immutable.");

                gen.popVar(this);
                freed = true;
            }
        }
    }
}
