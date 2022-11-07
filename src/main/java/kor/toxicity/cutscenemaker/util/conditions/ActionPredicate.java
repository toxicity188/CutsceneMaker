package kor.toxicity.cutscenemaker.util.conditions;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ActionPredicate<T> extends Predicate<T> {

    default ActionPredicate<T> castInstead(Consumer<T> action) {
        Objects.requireNonNull(action);
        return t -> {
            if (test(t)) {
                action.accept(t);
                return false;
            }
            return true;
        };
    }
    default ActionPredicate<T> cast(Consumer<T> action) {
        Objects.requireNonNull(action);
        return t -> {
            boolean r = test(t);
            if (r) action.accept(t);
            return r;
        };
    }

    default ActionPredicate<T> addAnd(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        return t -> test(t) && predicate.test(t);
    }
    default ActionPredicate<T> addOr(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        return t -> test(t) || predicate.test(t);
    }
}
