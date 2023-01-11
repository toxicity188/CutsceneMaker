package kor.toxicity.cutscenemaker.util.vars;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class Vars {

    private String var;

    public synchronized void setVar(String var) {
        this.var = var;
    }

    public synchronized String getVar() {
        return var;
    }

    public synchronized Number getAsNum() {
        return getAsNum(0D);
    }
    public synchronized Number getAsNum(Number def) {
        try {
            return Double.parseDouble(var);
        } catch (Exception e) {
            return def;
        }
    }

    public synchronized boolean getAsBool() {
        return getAsBool(false);
    }
    public synchronized boolean getAsBool(boolean def) {
        try {
            return Boolean.parseBoolean(var);
        } catch (Exception e) {
            return def;
        }
    }
}
