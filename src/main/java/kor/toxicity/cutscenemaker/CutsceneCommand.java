package kor.toxicity.cutscenemaker;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import kor.toxicity.cutscenemaker.commands.CommandHandler;
import kor.toxicity.cutscenemaker.commands.CommandListener;
import kor.toxicity.cutscenemaker.commands.CommandPacket;
import kor.toxicity.cutscenemaker.commands.SenderType;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.events.ActionReloadEndEvent;
import kor.toxicity.cutscenemaker.events.ActionReloadStartEvent;
import kor.toxicity.cutscenemaker.util.ConfigWriter;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CutsceneCommand implements CommandExecutor, TabCompleter {

    private static final String FALLBACK_PREFIX = "cutscenemaker";
    private static final Map<JavaPlugin, CommandRecord> listeners = new HashMap<>();
    private static final Set<PluginCommand> registeredCommand = new HashSet<>();
    private static SimpleCommandMap commandMap;
    private static Function<String,PluginCommand> createCommand;

    @SuppressWarnings("unchecked")
    public void unregister() {
        Field field = Arrays.stream(SimpleCommandMap.class.getDeclaredFields()).filter(f -> Map.class.isAssignableFrom(f.getType())).findFirst().orElse(null);
        try {
            if (field != null) {
                field.setAccessible(true);
                Map<String, Command> knownCommand = (Map<String, Command>) field.get(commandMap);
                registeredCommand.forEach(p -> {
                    knownCommand.remove(p.getName());
                    knownCommand.remove(FALLBACK_PREFIX + ":" + p.getName());
                    p.unregister(commandMap);
                });
                field.setAccessible(false);
                registeredCommand.clear();
            }
        } catch (Exception e) {
            CutsceneMaker.warn("unable to unregister command.");
        }
    }
    public static void createCommand(String name, CommandExecutor executor) {
        if (commandMap == null) return;
        try {
            PluginCommand command = createCommand.apply(name.toLowerCase());
            command.setDescription("generated command.");
            command.setLabel(name);
            command.setUsage("/" + name);
            command.setExecutor(executor);
            commandMap.register(FALLBACK_PREFIX,command);
            registeredCommand.add(command);
        } catch (Exception e) {
            CutsceneMaker.warn("unable to register command.");
        }
    }
    CutsceneCommand(CutsceneMaker pl) {
        try {
            Field map = Arrays.stream(Bukkit.getServer().getClass().getDeclaredFields()).filter(f -> SimpleCommandMap.class.isAssignableFrom(f.getType())).findFirst().orElse(null);
            if (map != null) {
                map.setAccessible(true);
                commandMap = (SimpleCommandMap) map.get(Bukkit.getServer());
                Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                constructor.setAccessible(true);
                createCommand = s -> {
                    try {
                        return constructor.newInstance(s, pl);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                };
            } else CutsceneMaker.warn("unable to find command map.");
        } catch (Exception e) {
            CutsceneMaker.warn("unable to load command map.");
        }

        register(pl, new CommandListener() {
            @CommandHandler(aliases = {"?????????"}, length = 0,description = "show list of registered commands.",usage = "/cutscene list",sender = {SenderType.CONSOLE, SenderType.PLAYER})
            public void list(CommandPacket pkg) {
                listeners.forEach((key, value) -> {
                    send(pkg.getSender(), ChatColor.GRAY + "-----< " + key.getName() + " >-----");
                    value.methods.keySet().forEach(t -> {
                        CommandHandler h = b(t);
                        send(pkg.getSender(), h.usage() + ChatColor.GRAY + " (" + c(Arrays.asList(h.aliases())) + ") " + ChatColor.GOLD + "- " + ChatColor.WHITE + h.description());
                    });
                });
            }
            @CommandHandler(aliases = {"re","rd","?????????"}, length = 0, description = "reload this plugin.", usage = "/cutscene reload", sender = {SenderType.CONSOLE, SenderType.PLAYER})
            public void reload(CommandPacket pkg) {
                EvtUtil.call(new ActionReloadStartEvent());
                pl.load(t -> send(pkg.getSender(),"load finished. (" + t + "ms)"));
                EvtUtil.call(new ActionReloadEndEvent());
            }
            @CommandHandler(aliases = {"??????","r"}, length = 1,description = "run Action.",usage = "/cutscene run <name>",sender = {SenderType.ENTITY})
            public void run(CommandPacket pkg) {
                boolean r = ActionData.start(pkg.getArgs()[1],(LivingEntity) pkg.getSender());
                if (!r) send(pkg.getSender(), "run failed.");
                else send(pkg.getSender(),"run success!");
            }
            @CommandHandler(aliases = {"?????????","i"}, length = 3,description = "get or set the item in the file data.",usage = "/cutscene item <get/set> <file> <key>",sender = {SenderType.PLAYER})
            public void item(CommandPacket pkg) {
                String[] args = pkg.getArgs();
                Player player = (Player) pkg.getSender();
                ItemStack item = player.getInventory().getItemInMainHand();
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
                                player.getInventory().addItem(new ItemBuilder(config.getItemStack(args[3])).get(player));
                                send(player, "successfully got.");
                            } else send(player, "item not found.");
                            break;
                        case "set":
                            if (item == null || item.getType() == Material.AIR) {
                                send(pkg.getSender(), "hold the item you want to save.");
                                return;
                            }
                            config.set(args[3],item);
                            config.save(dir);
                            send(player, "successfully saved.");
                            break;
                    }
                } catch (Exception e) {
                    send(player, "cannot save item.");
                }
            }
            @CommandHandler(aliases = {"??????","loc","l"}, length = 2,description = "save your location to file data.",usage = "/cutscene location <file> <key>",sender = {SenderType.PLAYER})
            public void location(CommandPacket pkg) {
                String[] args = pkg.getArgs();
                try {
                    ConfigWriter writer = new ConfigWriter(new File(pl.getDataFolder().getAbsolutePath() + "\\Locations\\" + args[1] + ".yml"));
                    writer.setLocation(args[2],((Player) pkg.getSender()).getLocation());
                    writer.save();
                    send(pkg.getSender(), "successfully saved.");
                } catch (Exception e) {
                    send(pkg.getSender(), "cannot save location.");
                }
            }
            @CommandHandler(aliases = {"tp"}, length = 1, description = "teleport to a registered location.", usage = "/cutscene teleport <name>", sender = SenderType.ENTITY)
            public void teleport(CommandPacket pkg) {
                String name = pkg.getArgs()[1];
                Location loc = pl.getManager().getLocations().getValue(name);
                if (loc != null) ((LivingEntity) pkg.getSender()).teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                else send(pkg.getSender(), "location \"" + name + "\" not found.");
            }
            @CommandHandler(aliases = {"var","v","??????"}, length = 3, description = "access to specific player's variables.", usage = "/cutscene variables <player> <name> <value>", sender = {SenderType.CONSOLE, SenderType.PLAYER})
            public void variables(CommandPacket pkg) {
                String[] args = pkg.getArgs();
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    send(pkg.getSender(),"unknown player's name.");
                } else {
                    VarsContainer container = pl.getManager().getVars(player);
                    if (container != null) {
                        Vars vars = container.get(args[2]);
                        if (args[3].equals("null")) {
                            send(pkg.getSender(), "successfully changed. (" + vars.getVar() + " to <none>)");
                            container.remove(args[2]);
                            return;
                        }
                        send(pkg.getSender(), "successfully changed. (" + vars.getVar() + " to " + args[3] + ")");
                        vars.setVar(args[3]);
                    }
                }
            }
            @CommandHandler(aliases = "m", length = 0, description = "get the material of item in sender's main hand", usage = "/cutscene material",sender = SenderType.PLAYER)
            public void material(CommandPacket pkg) {
                ItemStack item = ((Player) pkg.getSender()).getInventory().getItemInMainHand();
                if (item == null || item.getType() == null) {
                    send(pkg.getSender(),"hold the item you want to get the material from.");
                } else {
                    send(pkg.getSender(), "this item's material is: " + item.getType());
                }
            }
            @CommandHandler(length = 1, description = "show the variable list of specific player.", usage = "/cutscene log", sender = {SenderType.CONSOLE, SenderType.PLAYER})
            public void log(CommandPacket pkg) {
                String name = pkg.getArgs()[1];
                Player player = Bukkit.getPlayer(name);
                if (player == null) {
                    send(pkg.getSender(), "This player is not online!");
                    OfflinePlayer player1 = Arrays.stream(Bukkit.getOfflinePlayers()).filter(o -> o.getName().equals(name)).findFirst().orElse(null);
                    if (player1 != null) {
                        pl.getManager().runTaskAsynchronously(() -> {
                            Map<String,Vars> varsMap = new WeakHashMap<>();
                            try {
                                CSVReader reader = new CSVReader(new FileReader(pl.getDataFolder().getAbsolutePath() + "\\User\\" + player1.getUniqueId() + ".csv"));
                                reader.forEach(s -> {
                                    if (s.length > 1) varsMap.put(s[0],new Vars(s[1]));
                                });
                                reader.close();
                            } catch (Exception ignored) {}
                            send(pkg.getSender(), "This offline-player " + player1.getName() + "'s variable list. " + ChatColor.GRAY + "(" + player1.getUniqueId() + ")");
                            showVars(pkg.getSender(),varsMap);
                        });
                    } else send(pkg.getSender(), "Error: player not found");
                } else {
                    send(pkg.getSender(), "Player " + player.getName() + "'s variable list.");
                    VarsContainer container = pl.getManager().getVars(player);
                    if (container == null) {
                        send(pkg.getSender(), "Error: can't get " + player.getName() + "'s variable list.");
                        return;
                    }
                    showVars(pkg.getSender(),container.getVars());
                }
            }

            private void showVars(CommandSender sender, Map<String,Vars> varsMap) {
                List<String> vars = new ArrayList<>();
                List<String> quests = new ArrayList<>();
                varsMap.forEach((s,v) -> {
                    if (s.startsWith("quest.")) quests.add(ChatColor.GOLD + s.substring(6));
                    else vars.add(ChatColor.YELLOW + s + ChatColor.GRAY + ": " + ChatColor.GREEN + v.getVar());
                });
                send(sender,ChatColor.GRAY +"-----< " + ChatColor.BLUE + "Variables" + ChatColor.GRAY +" >-----");
                vars.forEach(s -> send(sender,s));
                send(sender,ChatColor.GRAY +"-----< " + ChatColor.BLUE + "Quests" + ChatColor.GRAY +" >-----");
                quests.forEach(s -> send(sender,s));
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
        sender.sendMessage(ChatColor.AQUA + "[CutsceneMaker] " + ChatColor.WHITE + m);
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
