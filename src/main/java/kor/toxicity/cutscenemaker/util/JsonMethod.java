package kor.toxicity.cutscenemaker.util;

import com.google.gson.JsonArray;

import java.util.function.Function;

@FunctionalInterface
public interface JsonMethod<T,R> {
    R apply(T t, JsonArray p);

    default Function<T, R> getAsFunction(JsonArray j) {
        return t -> apply(t,j);
    }
}
