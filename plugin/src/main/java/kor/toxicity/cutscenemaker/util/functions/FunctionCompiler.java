package kor.toxicity.cutscenemaker.util.functions;

import com.google.gson.JsonArray;

import java.util.function.Function;

public interface FunctionCompiler<T,R> {
    void initialize(JsonArray array);
    Function<T,R> compile();
}
