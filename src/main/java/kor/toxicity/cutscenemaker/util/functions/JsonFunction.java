package kor.toxicity.cutscenemaker.util.functions;

import com.google.gson.JsonArray;

import java.util.function.Function;

@FunctionalInterface
public interface JsonFunction<T,R> {
    R apply(T t, JsonArray p);

    default Function<T, R> getAsFunction(JsonArray j) {
        return t -> apply(t,j);
    }
}
