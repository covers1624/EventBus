package net.covers1624.eventbus.ap;

import net.covers1624.eventbus.api.EventFactory;
import net.covers1624.eventbus.api.EventListener;
import net.covers1624.eventbus.api.SubscribeEvent;
import net.covers1624.quack.collection.FastStream;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by covers1624 on 12/5/23.
 */
@SupportedAnnotationTypes ("*")
public class EventBusParameterProcessor extends AbstractProcessor {

    private final Map<String, Map<String, List<String>>> params = new LinkedHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (!env.processingOver()) {
            extractSubscribeEvent(env);
            extractFromClass(env.getRootElements(), EventFactory.class);
            extractFromClass(env.getRootElements(), EventListener.class);
            return false;
        }

        try {
            Filer filer = processingEnv.getFiler();
            for (Map.Entry<String, Map<String, List<String>>> entry : params.entrySet()) {
                String clazz = entry.getKey();
                Map<String, List<String>> methods = entry.getValue();
                if (methods.isEmpty()) continue;

                FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/eventbus/" + clazz.replace("/", ".") + ".params");
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(fileObject.openOutputStream(), StandardCharsets.UTF_8))) {
                    for (Map.Entry<String, List<String>> methodEntry : methods.entrySet()) {
                        pw.print(methodEntry.getKey());
                        pw.print(" ");
                        pw.println(String.join(",", methodEntry.getValue()));
                    }
                }
            }
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: Unable to create resource. " + ex);
        }
        return false;
    }

    private void extractSubscribeEvent(RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(SubscribeEvent.class)) {
            extractFromElement(element);
        }
    }

    private void extractFromClass(Iterable<? extends Element> elements, Class<?> superClazz) {
        for (Element element : elements) {
            if (!element.getKind().isInterface() && !element.getKind().isClass()) continue;
            if (extendsClass(element.asType(), superClazz)) {
                element.getEnclosedElements().forEach(this::extractFromElement);
            }
            extractFromClass(element.getEnclosedElements(), superClazz);
        }
    }

    private void extractFromElement(Element element) {
        if (element instanceof ExecutableElement) {
            extractFromElement((ExecutableElement) element);
        }
    }

    private void extractFromElement(ExecutableElement element) {
        // We only care about abstract methods.
        if (!element.getModifiers().contains(Modifier.ABSTRACT)) return;

        String owner = Utils.toInternalName(element.getEnclosingElement().asType());
        List<String> params = getMethodParamNames(element);
        if (params.isEmpty()) return;

        this.params.computeIfAbsent(owner, e -> new LinkedHashMap<>())
                .put(element.getSimpleName().toString() + getMethodDesc(element), params);
    }

    private List<String> getMethodParamNames(ExecutableElement element) {
        return FastStream.of(element.getParameters()).map(e -> e.getSimpleName().toString()).toList();
    }

    private String getMethodDesc(ExecutableElement element) {
        return "("
                + FastStream.of(element.getParameters())
                .map(e -> Utils.toInternalType(e.asType()))
                .join("")
                + ")"
                + Utils.toInternalType(element.getReturnType());
    }

    private boolean extendsClass(TypeMirror mirror, Class<?> superClazz) {
        String tName = superClazz.getName().replace(".", "/");
        Types types = processingEnv.getTypeUtils();
        for (TypeMirror directSupertype : types.directSupertypes(mirror)) {
            if (Utils.toInternalName(directSupertype).equals(tName)) {
                return true;
            }
        }
        return false;
    }
}
