package kor.toxicity.cutscenemaker.util.functions;

import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class FunctionArray implements Iterable<FunctionElement> {
    private final List<FunctionElement> objects = new ArrayList<>();
    public FunctionArray(JsonArray array) {
        array.forEach(e -> objects.add(new FunctionElement(e)));
    }
    public int size() {
        return objects.size();
    }
    public FunctionElement get(int i) {
        return objects.get(i);
    }

    @Override
    public Iterator<FunctionElement> iterator() {
        return objects.iterator();
    }
}
