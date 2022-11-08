package kor.toxicity.cutscenemaker.util.functions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodString {
    @Getter
    private static final MethodString instance = new MethodString();

    public MethodInterpreter parse(String s) {
        return new MethodInterpreter(s);
    }

}
