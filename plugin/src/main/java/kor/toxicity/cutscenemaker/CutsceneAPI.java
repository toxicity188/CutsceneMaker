package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.action.mechanic.ActSlate;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.quests.Dialog;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.quests.QuestSet;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class CutsceneAPI {

    @NotNull
    private final JavaPlugin pl;

    public void addAction(@NotNull String name,@NotNull Class<? extends CutsceneAction> action) {
        ActionData.addAction(name,action);
    }

    public void addEventHandler(@NotNull String string,@NotNull Class<? extends ActionHandler> clazz) {
        ActionHandler.addHandler(string,clazz);
    }
    public void addSlateOffTask(@NotNull Consumer<Player> consumer) {
        ActSlate.addSlateOffTask(Objects.requireNonNull(consumer));
    }
    public void addSlateOnTask(@NotNull Consumer<Player> consumer) {
        ActSlate.addSlateOnTask(Objects.requireNonNull(consumer));
    }

    public void runAction(@NotNull String name,@NotNull LivingEntity entity) {
        ActionData.start(name,entity);
    }
    public @Nullable Dialog getDialog(String name) {
        return QuestData.getDialog(name);
    }
    public @Nullable QuestSet getQuestSet(String name) {
        return QuestData.getQuestSet(name);
    }

}
