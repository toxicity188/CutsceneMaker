package kor.toxicity.cutscenemaker.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public enum SenderType {

    CONSOLE(ConsoleCommandSender.class),
    PLAYER(Player.class),
    ENTITY(Entity.class),
    ;

    public final Class<? extends CommandSender> sender;

    SenderType(Class<? extends CommandSender> s) {
        sender = s;
    }
}
