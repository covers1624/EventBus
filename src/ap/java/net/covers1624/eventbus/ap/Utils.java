package net.covers1624.eventbus.ap;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Created by covers1624 on 28/7/21.
 */
public class Utils {

    //Builds the Internal type name for this TypeMirror.
    public static String toInternalType(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "Z";
            case BYTE:
                return "B";
            case SHORT:
                return "S";
            case INT:
                return "I";
            case LONG:
                return "J";
            case CHAR:
                return "C";
            case FLOAT:
                return "F";
            case DOUBLE:
                return "D";
            case ARRAY:
                return "[" + toInternalType(((ArrayType) type).getComponentType());
            case DECLARED:
                return "L" + buildDeclaredName(((DeclaredType) type).asElement()) + ";";
            case VOID:
                return "V";
            default:
                throw new UnsupportedOperationException("Unhandled TypeMirror kind." + type.getKind());
        }
    }

    //Builds the Internal name for a given element. This assumes the name is a Class/Interface/Enum.
    public static String buildDeclaredName(Element element) {
        StringBuilder name = new StringBuilder(element.getSimpleName().toString());
        while ((element = element.getEnclosingElement()) != null) {
            if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE || element.getKind() == ElementKind.ENUM) {
                name.insert(0, element.getSimpleName() + "$");
            } else {
                name.insert(0, element.toString().replace(".", "/") + "/");
            }
        }
        return name.toString();
    }
}
