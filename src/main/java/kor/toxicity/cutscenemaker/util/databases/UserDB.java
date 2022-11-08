package kor.toxicity.cutscenemaker.util.databases;

import org.bukkit.entity.Player;

public interface UserDB {
    void save(Player player);
    void load(Player player);
    boolean isLoaded();
    void initialize();
}
