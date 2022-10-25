package kor.toxicity.cutscenemaker.util.conditions;

@FunctionalInterface
public interface ComparisonOperator<T> {

    boolean get(T t, T r);

}
