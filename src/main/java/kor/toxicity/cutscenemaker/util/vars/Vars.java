package kor.toxicity.cutscenemaker.util.vars;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Vars {

    private String var;

    public Number getAsNum() {
        return getAsNum(0D);
    }
    public Number getAsNum(Number def) {
        try {
            return Double.parseDouble(var);
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getAsBool() {
        return getAsBool(false);
    }
    public boolean getAsBool(boolean def) {
        try {
            return Boolean.parseBoolean(var);
        } catch (Exception e) {
            return def;
        }
    }
}
