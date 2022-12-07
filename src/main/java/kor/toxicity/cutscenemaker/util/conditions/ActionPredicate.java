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
            if (test(t)) action.accept(t);
            return true;
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

    default ActionPredicate<T> match(String action, Predicate<T> predicate) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(predicate);
        switch (action) {
            case "&&":
                return addAnd(predicate);
            case "||":
                return addOr(predicate);
        }
        return this;
    }
}
