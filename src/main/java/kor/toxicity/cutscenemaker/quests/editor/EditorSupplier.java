package kor.toxicity.cutscenemaker.quests.editor;

import kor.toxicity.cutscenemaker.util.gui.GuiExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public interface EditorSupplier {

    Editor getEditor(Player player);
    interface Editor {
        void updateGui();
        GuiExecutor getMainExecutor();
        ConfigurationSection getSaveData();
    }

}
