package kor.toxicity.cutscenemaker.actions;

import org.bukkit.entity.LivingEntity;

import java.util.function.BiConsumer;

public interface EntityRegister {

    void getEntity(BiConsumer<String, LivingEntity> callback);

}