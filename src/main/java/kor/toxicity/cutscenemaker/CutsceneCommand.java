package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.commands.CommandHandler;
import kor.toxicity.cutscenemaker.commands.CommandListener;
import kor.toxicity.cutscenemaker.commands.CommandPacket;
import kor.toxicity.cutscenemaker.commands.SenderType;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.util.ConfigWriter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class CutsceneCommand implements CommandExecutor, TabCompleter {

    private static final Map<JavaPlugin, CommandRecord> listeners = new HashMap<>();

    CutsceneCommand(CutsceneMaker pl) {
        register(pl, new CommandListener() {
            @CommandHandler(aliases = {"l", "리스트"}, length = 0,description = "show list of registered commands.",usage = "/cutscene list",sender = {SenderType.CONSOLE, SenderType.PLAYER})
            public void list(CommandPacket pkg) {
                listeners.forEach((key, value) -> {
                    send(pkg.getSender(), ChatColor.GRAY + "-----< " + key.getName() + " >-----");
                    value.methods.keySet().forEach(t -> {
                        CommandHandler h = b(t);
                        send(pkg.getSender(), h.usage() + ChatColor.GRAY + " (" + c(Arrays.asList(h.aliases())) + ") " + ChatColor.GOLD + "- " + ChatColor.WHITE + h.description());
                    });
                });
            }
            @CommandHandler(aliases = {"re","rd","리로드"}, length = 0, description = "reload this plugin.", usage = "/cutscene reload", sender = {SenderType.CONSOLE, SenderType.PLAYER})
            public void reload(CommandPacket pkg) {
                long time = System.currentTimeMillis();
                pl.load();
                time -= System.currentTimeMillis();
                send(pkg.getSender(),"load finished. (" + -time + "ms)");
            }
            @CommandHandler(aliases = "실행", length = 1,description = "run Action that has loaded.",usage = "/cutscene run <name>",sender = {SenderType.ENTITY})
            public void run(CommandPacket pkg) {
                boolean r = ActionData.start(pkg.getArgs()[1],(LivingEntity) pkg.getSender());
                if (!r) send(pkg.getSender(), "run failed.");
                else send(pkg.getSender(),"run success!");
            }
            @CommandHandler(aliases = "아이템", length = 3,description = "get or set item.",usage = "/cutscene item <get/set> <file> <key>",sender = {SenderType.PLAYER})
            public void item(CommandPacket pkg) {
                String[] args = pkg.getArgs();
                Player player = (Player) pkg.getSender();
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    send(pkg.getSender(), "hand item you want to save.");
                    return;
                }
                try {
                    YamlConfiguration config = new YamlConfiguration();
                    File dir = new File(pl.getDataFolder().getAbsolutePath() + "\\Items\\" + args[2] + ".yml");
                    if (!dir.exists()) dir.createNewFile();
                    config.load(dir);
                    switch (args[1]) {
                        default:
                            send(player, "\"get\" or \"set\" required.");
                            break;
                        case "get":
                            if (config.isSet(args[3]) && config.isItemStack(args[3])) {
                                player.getInventory().addItem(config.getItemStack(args[3]));
                                send(player, "successfully get.");
                            } else send(player, "item not found.");
                            break;
                        case "set":
                            config.set(args[3],item);
                            config.save(dir);
                            send(player, "successfully saved.");
                            break;
                    }
                } catch (Exception e) {
                    send(player, "sorry, cannot save item.");
                }
            }
            @CommandHandler(aliases = {"좌표","loc"}, length = 2,description = "save your location to file data.",usage = "/cutscene location <file> <key>",sender = {SenderType.PLAYER})
            public void location(CommandPacket pkg) {
                String[] args = pkg.getArgs();
                try {
                    ConfigWriter writer = new ConfigWriter(new File(pl.getDataFolder().getAbsolutePath() + "\\Locations\\" + args[1] + ".yml"));
                    writer.setLocation(args[2],((Player) pkg.getSender()).getLocation());
                    writer.save();
                    send(pkg.getSender(), "successfully saved.");
                } catch (Exception e) {
                    send(pkg.getSender(), "sorry, cannot save location.");
                }
            }
        });
    }
    private String c(List<String> l) {
        StringBuilder a = new StringBuilder();
        int loop = 0;
        for (String s : l) {
            a.append(s);
            loop ++;
            if (loop < l.size()) a.append(",");
        }
        return a.toString();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        Method m;
        if (strings.length == 0) strings = new String[] {"list"};
        for (CommandRecord value : listeners.values()) {
            m = value.a(strings[0]);
            if (m != null) {
                try {
                    CommandHandler h = b(m);
                    if (h.length() + 1 > strings.length) {
                        send(commandSender,"usage : " + h.usage());
                        return true;
                    }
                    if (Arrays.stream(h.sender()).map(b -> b.sender).noneMatch(c -> c.isAssignableFrom(commandSender.getClass()))) {
                        send(commandSender,"available sender : " + c(Arrays.stream(h.sender()).map(b -> b.toString().toLowerCase()).collect(Collectors.toList())));
                        return true;
                    }
                    if (!commandSender.isOp() && h.opOnly()) {
                        send(commandSender, "sorry, this is an OP only command.");
                        return true;
                    }
                    m.invoke(value.methods.get(m),new CommandPacket(commandSender,strings));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        }
        send(commandSender,"unknown command. chat \"/cutscene list\" to find commands");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            List<String> t = new ArrayList<>();
            listeners.values().forEach(b -> t.addAll(b.methods.keySet().stream().map(Method::getName).collect(Collectors.toList())));
            return t;
        }
        return null;
    }

    public static void register(JavaPlugin plugin, CommandListener listener) {
        List<Method> methods = Arrays.stream(listener.getClass().getDeclaredMethods()).filter(m -> b(m) != null).collect(Collectors.toList());
        if (!methods.isEmpty()) {
            Set<Method> t = new HashSet<>();
            methods.stream().filter(m -> m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == CommandPacket.class).forEach(t::add);

            if (!listeners.containsKey(plugin)) listeners.put(plugin, new CommandRecord());
            listeners.get(plugin).add(t, listener);
        }
    }

    private static void send(CommandSender sender, String m) {
        sender.sendMessage(ChatColor.AQUA + "[CutSceneMaker] " + ChatColor.WHITE + m);
    }

    private static class CommandRecord {
        private final Map<Method,CommandListener> methods;

        private CommandRecord() {
            methods = new HashMap<>();
        }
        private void add(Set<Method> a, CommandListener b) {
            a.forEach(m -> methods.put(m,b));
        }

        private Method a(String s) {
            return methods.keySet().stream().filter(k -> k.getName().equals(s) || Arrays.asList(b(k).aliases()).contains(s)).findFirst().orElse(null);
        }

    }

    private static CommandHandler b(Method m) {
        return m.getDeclaredAnnotation(CommandHandler.class);
    }
}
