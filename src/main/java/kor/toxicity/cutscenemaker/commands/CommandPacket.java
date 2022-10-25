package kor.toxicity.cutscenemaker.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public final class CommandPacket {

    private final CommandSender sender;
    private final String[] args;

    public List<String> getListArgs() {
        return Arrays.asList(args);
    }
}
