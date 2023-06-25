package kor.toxicity.cutscenemaker.util.functions;

import com.google.gson.JsonArray;

import java.util.function.Function;

public abstract class CompileFunction<T,R> implements CheckableFunction<T,R> {
    public abstract FunctionCompiler<T,R> initialize();

    @Override
    public R apply(T t, JsonArray array) {
        FunctionCompiler<T,R> compiler = initialize();
        compiler.initialize(array);
        return compiler.compile().apply(t);
    }

    @Override
    public Function<T, R> getAsFunction(JsonArray j) {
        FunctionCompiler<T,R> compiler = initialize();
        compiler.initialize(j);
        return compiler.compile();
    }
}
