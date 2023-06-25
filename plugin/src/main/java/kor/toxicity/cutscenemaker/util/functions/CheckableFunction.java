package kor.toxicity.cutscenemaker.util.functions;

import com.google.gson.JsonArray;

public interface CheckableFunction<T,R> extends JsonFunction<T,R> {
    boolean check(JsonArray array);
}
