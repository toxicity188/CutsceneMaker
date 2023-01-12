package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

class MessageSender {
    private final FunctionPrinter printer;
    private final Consumer<Player> soundPlay;

    static MessageSender toConfig(ConfigurationSection section, String key) {
        try {
            return new MessageSender(section,key);
        } catch (Exception e) {
            CutsceneMaker.warn(e.getMessage());
            return null;
        }
    }
    private MessageSender(ConfigurationSection section, String key) {
        if (!section.isSet(key)) throw new RuntimeException("Invalid statement: " + key);
        if (section.isConfigurationSection(key)) {
            ConfigurationSection get = section.getConfigurationSection(key);
            String sound = get.getString("sound",null);
            soundPlay = (sound != null) ? QuestUtil.getInstance().getSoundPlay(sound) : null;
            String msg = get.getString("message",null);
            if (msg == null) throw new RuntimeException("Unable to find the message value: " + key);
            printer = new FunctionPrinter(msg);
        } else {
            printer = new FunctionPrinter(section.getString(key));
            soundPlay = null;
        }
    }
    void send(Player player) {
        player.sendMessage(printer.print(player));
        if (soundPlay != null) soundPlay.accept(player);
    }
    void send(Player player, String add) {
        player.sendMessage(printer.print(player) + add);
        if (soundPlay != null) soundPlay.accept(player);
    }
}
