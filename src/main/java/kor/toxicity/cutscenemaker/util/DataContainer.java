package kor.toxicity.cutscenemaker.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataContainer<T> {

    private final Map<String,DataNode> node = new LinkedHashMap<>();

    private class DataNode {
        private final Map<String,T> value = new LinkedHashMap<>();
    }

    public boolean containsNodeKey(String key) {
        return node.containsKey(key);
    }
    public boolean containsKey(String key) {
        return node.values().stream().anyMatch(t -> t.value.containsKey(key));
    }
    public T getValue(String name) {
        return node.values().stream().filter(t -> t.value.containsKey(name)).findFirst().map(v -> v.value.get(name)).orElse(null);
    }
    public Collection<T> getValues(String file) {
        return (node.containsKey(file)) ? node.get(file).value.values() : null;
    }
    public void clear() {
        for (DataNode value : node.values()) value.value.clear();
        node.clear();
    }
    public int size() {
        return node.values().stream().mapToInt(m -> m.value.size()).sum();
    }
    public void put(String file, String key, T value) {
        if (!node.containsKey(file)) node.put(file,new DataNode());
        node.get(file).value.put(key,value);
    }
    public void forEach(Function<T> function) {
        for (Map.Entry<String, DataNode> f : node.entrySet()) {
            for (Map.Entry<String, T> k : f.getValue().value.entrySet()) {
                function.accept(f.getKey(),k.getKey(),k.getValue());
            }
        }
    }

    public interface Function<T> {
        void accept(String file, String key, T value);
    }
}
