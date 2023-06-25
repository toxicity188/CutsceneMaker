package kor.toxicity.cutscenemaker.util.functions;

@FunctionalInterface
public interface ComparisonOperator<T> {

    boolean get(T t, T r);

}
