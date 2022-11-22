package kor.toxicity.cutscenemaker.util.functions;

import com.google.gson.JsonElement;
import org.bukkit.entity.LivingEntity;

public class FunctionElement {
    private final MethodInterpreter interpreter;
    public FunctionElement(JsonElement element) {
        interpreter = MethodString.getInstance().parse(element.getAsString());
    }
    public String getAsString(LivingEntity entity) {
        return interpreter.print(entity);
    }
    public double getAsDouble(LivingEntity entity) {
        try {
            Double.parseDouble(interpreter.print(entity));
        } catch (Exception ignored) {
        }
        return 0D;
    }
}
