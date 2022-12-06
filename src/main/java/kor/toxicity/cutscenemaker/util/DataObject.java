package kor.toxicity.cutscenemaker.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.util.functions.MethodInterpreter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class DataObject<T> {

    private final T t;
    private List<String> check;

    public DataObject(T t) {
        this.t = t;
    }


    public void apply(JsonObject j) {
        List<Field> f = c();
        j.entrySet().forEach(e ->  f.stream().filter(a -> a.getName().equals(e.getKey()) || Arrays.asList(a(a).aliases()).contains(e.getKey())).findFirst().ifPresent(b -> b(t).accept(b, e.getValue())));
        check(f);
    }
    public boolean isLoaded() {
        if (check == null) check(null);
        return check.isEmpty();
    }
    private void check(List<Field> recycle) {
        check = ((recycle != null) ? recycle : c()).stream().filter(a -> {
            try {
                return a(a).throwable() && a.get(t) == null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).map(Field::getName).collect(Collectors.toList());
    }

    public List<String> getErrorField() {
        return check;
    }


    private DataField a(Field t) {
        return t.getDeclaredAnnotation(DataField.class);
    }
    private BiConsumer<Field, JsonElement> b(T a) {
        return (f,j) -> {
            try {
                Object p = null;
                if (f.getType() == Integer.TYPE) p = j.getAsInt();
                if (f.getType() == Double.TYPE) p = j.getAsDouble();
                if (f.getType() == Float.TYPE) p = j.getAsFloat();
                if (f.getType() == Boolean.TYPE) p = j.getAsBoolean();
                if (f.getType() == String.class) p = j.getAsString();
                if (f.getType() == JsonObject.class && j.isJsonObject()) p = j.getAsJsonObject();
                if (f.getType() == MethodInterpreter.class) p = new MethodInterpreter(j.getAsString());
                f.set(a, p);
            } catch (IllegalAccessException ignored) {}
        };
    }
    private List<Field> c() {
        return Arrays.stream(t.getClass().getFields()).filter(f -> a(f) != null).collect(Collectors.toList());
    }
}
