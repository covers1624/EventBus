package net.covers1624.eventbus.ap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.covers1624.eventbus.api.EventInvoker;
import net.covers1624.eventbus.api.SubscribeEvent;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.covers1624.eventbus.ap.Utils.toInternalType;

/**
 * Created by covers1624 on 9/4/21.
 */
@SupportedAnnotationTypes("*")
public class EventBusAnnotationProcessor extends AbstractProcessor {

    private static final String EVENT_INVOKER = "L" + EventInvoker.class.getName().replace(".", "/") + ";";

    private static final Type parametersToken = new TypeToken<Map<String, Map<String, List<String>>>>() {}.getType();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Map<String, List<String>>> methodParameters = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            String resourceFile = "META-INF/net/covers1624/eventbus/params.json";
            Filer filer = processingEnv.getFiler();
            try {
                //Attempt to load an existing params.json (incremental compilation)
                FileObject fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
                try (Reader reader = fileObject.openReader(false)) {
                    Map<String, Map<String, List<String>>> existing = gson.fromJson(reader, parametersToken);
                    for (Map.Entry<String, Map<String, List<String>>> classEntry : existing.entrySet()) {
                        String className = classEntry.getKey();
                        Map<String, List<String>> methods = methodParameters.computeIfAbsent(className, e -> new HashMap<>());
                        for (Map.Entry<String, List<String>> methodEntry : classEntry.getValue().entrySet()) {
                            String methodNameDesc = methodEntry.getKey();
                            if (!methods.containsKey(methodNameDesc)) {
                                methods.put(methodNameDesc, methodEntry.getValue());
                            }
                        }
                    }
                } catch (IOException ignored) {
                }

                //Write the new json.
                fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileObject.openOutputStream(), StandardCharsets.UTF_8))) {
                    gson.toJson(methodParameters, writer);
                    writer.flush();
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: Unable to create " + resourceFile + ", " + e);
            }
        } else {
            extractSubscribeEvent(roundEnv);
            extractInvokers(roundEnv.getRootElements());
        }
        return false;
    }

    private void extractSubscribeEvent(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(SubscribeEvent.class)) {
            processMethodMember(element);
        }
    }

    private void extractInvokers(Collection<? extends Element> elements) {
        for (Element element : elements) {
            if (element.getKind() != ElementKind.INTERFACE) continue;
            if (extendsEventInvoker(element.asType())) {
                for (Element member : element.getEnclosedElements()) {
                    processMethodMember(member);
                }
            }
            extractInvokers(element.getEnclosedElements());
        }
    }

    //Process the given element
    private void processMethodMember(Element _element) {
        if (_element instanceof ExecutableElement) {
            ExecutableElement element = (ExecutableElement) _element;
            String owner = toInternalType(element.getEnclosingElement().asType());
            owner = owner.substring(1, owner.length() - 1);
            Map<String, List<String>> methods = methodParameters.computeIfAbsent(owner, e -> new HashMap<>());
            StringBuilder nameBuilder = new StringBuilder(element.getSimpleName());
            nameBuilder.append('(');
            List<String> parameters = new ArrayList<>();
            for (VariableElement parameter : element.getParameters()) {
                nameBuilder.append(toInternalType(parameter.asType()));
                parameters.add(parameter.getSimpleName().toString());
            }
            nameBuilder.append(')');
            nameBuilder.append(toInternalType(element.getReturnType()));
            methods.computeIfAbsent(nameBuilder.toString(), e -> new ArrayList<>()).addAll(parameters);
        }
    }

    private boolean extendsEventInvoker(TypeMirror mirror) {
        if (Utils.toInternalType(mirror).equals(EVENT_INVOKER)) return true;

        Types types = processingEnv.getTypeUtils();
        for (TypeMirror directSupertype : types.directSupertypes(mirror)) {
            if (extendsEventInvoker(directSupertype)) return true;
        }

        return false;
    }
}
