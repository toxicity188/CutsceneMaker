package kor.toxicity.cutscenemaker.actions.mechanics;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.WeakHashMap;

public class ActLazyWarp extends CutsceneAction {

    private static final ConfigurationSection WARP_EFFECT = new MemoryConfiguration();
    private static final Map<Player,LazyWarpCallback> CALLBACK_MAP = new WeakHashMap<>();
    static {
        set("color","00FFFF");
        set("orient",true);
        set("orientPitch",false);
        set("particles",1);
        set("particles2",8);
        set("xEquation","0");
        set("yEquation","0.25");
        set("zEquation","0");

    }
    private static void set(String key, Object object) {
        WARP_EFFECT.set(key, object);
    }
    private static void remove(Player player) {
        LazyWarpCallback callback = CALLBACK_MAP.remove(player);
        if (callback != null) callback.cancel();
    }

    private static Listener listener;
    public ActLazyWarp(CutsceneManager pl) {
        super(pl);
        if (listener == null) {
            listener = new Listener() {
                @EventHandler
                public void move(PlayerMoveEvent e) {
                    LazyWarpCallback callback = CALLBACK_MAP.get(e.getPlayer());
                    if (callback != null) {
                        Player player = e.getPlayer();
                        Location location = e.getTo();
                        if (!callback.location.getWorld().equals(location.getWorld())) {
                            remove(player);
                            return;
                        }
                        if (callback.location.distance(location) > callback.size) {
                            remove(player);
                            if (callback.fail != null) ActionData.start(callback.fail.print(player),player);
                        }
                    }
                }
                @EventHandler
                public void quit(PlayerQuitEvent e) {
                    remove(e.getPlayer());
                }
            };
            pl.registerEvent(listener);
        }
    }
    @DataField(aliases = {"l","loc"},throwable = true)
    public String location;
    @DataField(aliases = "f")
    public FunctionPrinter fail;

    @DataField(aliases = "c")
    public String color;
    @DataField(aliases = "p")
    public String particle;
    @DataField
    public FunctionPrinter success;
    @DataField(aliases = "i")
    public int interval = 5;
    @DataField(aliases = "s")
    public double size = 1;

    private Location warp;
    private final ConfigurationSection section1 = new MemoryConfiguration();
    private final ConfigurationSection section2 = new MemoryConfiguration();

    @Override
    public void initialize() {
        super.initialize();
        warp = manager.getLocations().getValue(location);
        copy(section1);
        copy(section2);

        int tick = interval * 20;
        if (size < 1) size = 1;

        if (color != null) {
            section1.set("color",color);
            section2.set("color",color);
        }
        if (particle != null) {
            section1.set("particle",particle);
            section2.set("particle",particle);
        }
        section1.set("x2Equation",size + "cos((t2/4 + t/20) * 3.1415)");
        section1.set("z2Equation",size + "sin((t2/4 + t/20) * 3.1415)");
        section1.set("y2Equation",2 * size + " - t / " + tick  / size);
        section1.set("duration", tick * 50);

        section2.set("x2Equation",size + "cos((t2/4 - t/20) * 3.1415)");
        section2.set("z2Equation",size + "sin((t2/4 - t/20) * 3.1415)");
        section2.set("y2Equation","t / " + tick  / size);
        section2.set("duration", tick * 50);


        if (warp == null) CutsceneMaker.warn("The Location named \"" + location + " doesn't exist!");
        if (interval < 1) interval = 1;
    }

    private static void copy(ConfigurationSection section) {
        WARP_EFFECT.getKeys(false).forEach(s -> section.set(s,WARP_EFFECT.get(s)));
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (warp != null && entity instanceof Player) {
            Location location = entity.getLocation();
            EffectManager effectManager = manager.getEffectLib();
            Player p = (Player) entity;
            CALLBACK_MAP.put(p, new LazyWarpCallback(
                    p,
                    location,
                    fail,
                    effectManager.start(
                            "EquationEffect",
                            section1,
                            location
                    ),
                    effectManager.start(
                            "EquationEffect",
                            section2,
                            location
                    ),
                    size
            ));
        }
    }

    private class LazyWarpCallback {
        private final Location location;
        private final FunctionPrinter fail;
        private final Effect effect1, effect2;
        private final BukkitTask counting;
        private final double size;

        private int count;

        private LazyWarpCallback(Player player, Location location, FunctionPrinter fail, Effect effect1, Effect effect2, double size) {
            this.location = location;
            this.fail = fail;
            this.effect1 = effect1;
            this.effect2 = effect2;
            this.size = size;
            counting = manager.runTaskTimer(() -> {
                if (++count == interval) {
                    remove(player);
                    player.teleport(warp, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    if (success != null) ActionData.start(success.print(player),player);
                }
            },20,20);
        }

        private void cancel() {
            effect1.cancel();
            effect2.cancel();
            counting.cancel();
        }
    }
}
