package kor.toxicity.cutscenemaker.util.conditions;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ActionPredicate<T> extends Predicate<T> {

    default Predicate<T> withAction(Consumer<T> action, boolean cancel) {
        return t -> {
            if (!test(t)) {
                action.accept(t);
                return !cancel;
            }
            return true;
        };
    }
}
