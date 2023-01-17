package kor.toxicity.cutscenemaker.util.reflect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataObject {

    private final Object t;
    private final String name;
    private List<String> check;

    public DataObject(@NotNull Object t, @Nullable String name) {
        this.t = Objects.requireNonNull(t);
        this.name = name;
    }


    public void apply(JsonObject j) {
        List<Field> f = c();
        j.entrySet().forEach(e ->  {
            Field b = f.stream().filter(a -> a.getName().equals(e.getKey()) || Arrays.asList(a(a).aliases()).contains(e.getKey())).findFirst().orElse(null);
            if (b != null) b().accept(b, e.getValue());
            else CutsceneMaker.warn("The Field named \"" + e.getKey() + "\" doesn't exist" + parseName());
        });
        check(f);
    }
    private String parseName() {
        return (name != null) ? " in " + name + "!" : "!";
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
    private BiConsumer<Field, JsonElement> b() {
        return (f,j) -> {
            DataType type = DataType.findByClass(f.getType());
            if (type != null) {
                try {
                    f.set(t, type.converter.apply(j));
                } catch (Exception e) {
                    CutsceneMaker.warn("Syntax error: " + j.getAsString() + " cannot be converted to type " + type.name + parseName());
                }
            } else CutsceneMaker.warn("No type converter found: " + f.getType().toGenericString() + parseName());
        };
    }
    private List<Field> c() {
        return Arrays.stream(t.getClass().getFields()).filter(f -> a(f) != null).collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    private enum DataType {
        BYTE(Byte.TYPE,"byte",JsonElement::getAsByte),
        SHORT(Short.TYPE,"short",JsonElement::getAsShort),
        INTEGER(Integer.TYPE,"int",JsonElement::getAsInt),
        DOUBLE(Double.TYPE, "double",JsonElement::getAsDouble),
        FLOAT(Float.TYPE, "float",JsonElement::getAsFloat),
        BOOLEAN(Boolean.TYPE, "boolean",JsonElement::getAsBoolean),
        STRING(String.class, "string",JsonElement::getAsString),
        JSON_OBJECT(JsonObject.class, "{object}",JsonElement::getAsJsonObject),
        JSON_ARRAY(JsonArray.class, "[array]",JsonElement::getAsJsonArray),
        FUNCTION_PRINTER(FunctionPrinter.class, "String (Function)",j -> new FunctionPrinter(j.getAsString())),
        ;
        final Class<?> type;
        final String name;
        final Function<JsonElement,Object> converter;
        private static DataType findByClass(Class<?> clazz) {
            return Arrays.stream(values()).filter(d -> d.type == clazz).findFirst().orElse(null);
        }
    }
}
