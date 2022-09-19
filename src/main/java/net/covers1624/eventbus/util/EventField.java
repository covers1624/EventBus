package net.covers1624.eventbus.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an event field.
 * Event Fields have a name, getter, and possibly a setter.
 * <p>
 * Created by covers1624 on 10/4/21.
 */
public class EventField {

    public final String name;
    public final Class<?> type;
    public final Type genericType;
    public final Method getter;
    @Nullable
    public final Method setter;

    public EventField(String name, Class<?> type, Type genericType, Method getter, @Nullable Method setter) {
        this.name = name;
        this.type = type;
        this.genericType = genericType;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean isImmutable() {
        return setter == null;
    }

    public org.objectweb.asm.Type getType() {
        return org.objectweb.asm.Type.getType(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventField that = (EventField) o;

        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;
        if (!genericType.equals(that.genericType)) return false;
        if (!getter.equals(that.getter)) return false;
        return Objects.equals(setter, that.setter);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + genericType.hashCode();
        result = 31 * result + getter.hashCode();
        result = 31 * result + (setter != null ? setter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EventField.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("type='" + type + "'")
                .add("genericType='" + genericType + "'")
                .add("getter=" + getter)
                .add("setter=" + setter)
                .toString();
    }
}
