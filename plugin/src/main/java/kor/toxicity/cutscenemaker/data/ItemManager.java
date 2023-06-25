package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.util.ItemBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ItemManager {
    @Nullable ItemBuilder getItem(String name);
    boolean contains(String name);
    Set<String> keySet();
}
