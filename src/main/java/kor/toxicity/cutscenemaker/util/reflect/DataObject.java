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
        BYTE(new Class[] {Byte.TYPE, Byte.class},"byte",JsonElement::getAsByte),
        SHORT(new Class[] {Short.TYPE, Short.class},"short",JsonElement::getAsShort),
        INTEGER(new Class[] {Integer.TYPE, Integer.class},"int",JsonElement::getAsInt),
        DOUBLE(new Class[] {Double.TYPE, Double.class}, "double",JsonElement::getAsDouble),
        FLOAT(new Class[] {Float.TYPE, Float.class}, "float",JsonElement::getAsFloat),
        LONG(new Class[] {Long.TYPE, Long.class}, "long",JsonElement::getAsLong),
        BOOLEAN(new Class[] {Boolean.TYPE, Boolean.class}, "boolean",JsonElement::getAsBoolean),
        CHARACTER(new Class[] {Character.TYPE, Character.class}, "character",JsonElement::getAsCharacter),
        STRING(new Class[] {String.class}, "string",JsonElement::getAsString),
        JSON_OBJECT(new Class[] {JsonObject.class}, "{object}",JsonElement::getAsJsonObject),
        JSON_ARRAY(new Class[] {JsonArray.class}, "[array]",j -> {
            if (j.isJsonArray()) return j.getAsJsonArray();
            else {
                JsonArray array = new JsonArray();
                array.add(j.getAsString());
                return array;
            }
        }),
        FUNCTION_PRINTER(new Class[] {FunctionPrinter.class}, "String (Function)",j -> new FunctionPrinter(j.getAsString()))
        ;
        final Class<?>[] types;
        final String name;
        final Function<JsonElement,Object> converter;
        private static DataType findByClass(Class<?> clazz) {
            return Arrays.stream(values()).filter(d -> Arrays.stream(d.types).anyMatch(t -> t == clazz)).findFirst().orElse(null);
        }
    }
}
