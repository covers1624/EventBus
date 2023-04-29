package net.covers1624.eventbus.util;

import com.google.common.collect.ImmutableMap;
import net.covers1624.eventbus.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 24/7/22.
 */
public class ASMParameterParser {

    private static final Logger LOGGER = LogManager.getLogger();

    public static Map<String, List<String>> parseParams(Environment env, Class<?> clazz) {
        try (InputStream is = env.getClassStream(clazz)) {
            if (is != null) {
                return parse(is);
            }
        } catch (IOException ex) {
            LOGGER.debug("Failed to read class: {}", clazz.getName(), ex);
        }
        return ImmutableMap.of();
    }

    @Nullable
    private static Map<String, List<String>> parse(InputStream is) throws IOException {
        ClassReader reader = new ClassReader(is);
        MethodParamClassVisitor visitor = new MethodParamClassVisitor();
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return visitor.methodParamMap;
    }

    private static class MethodParamClassVisitor extends ClassVisitor {

        public final Map<String, List<String>> methodParamMap = new HashMap<>();

        public MethodParamClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            int paramWidth = getParamWidth(descriptor);
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;

            List<String> params = new ArrayList<>();
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                    if ((index != 0 || isStatic) && index < (isStatic ? paramWidth : paramWidth + 1)) {
                        params.add(name);
                    }
                }

                @Override
                public void visitEnd() {
                    if (!params.isEmpty()) {
                        methodParamMap.put(name + descriptor, params);
                    }
                }
            };
        }

        private static int getParamWidth(String desc) {
            int width = 0;
            for (Type arg : Type.getMethodType(desc).getArgumentTypes()) {
                width += arg.getSize();
            }
            return width;
        }
    }
}
