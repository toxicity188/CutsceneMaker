package kor.toxicity.cutscenemaker.actions.mechanics;

import com.google.gson.JsonArray;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.functions.CheckableFunction;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActStatic extends CutsceneAction {

    private static final Map<String, Player> STATIC_MAP = new ConcurrentHashMap<>();
    private static ListenerManager manager;

    @DataField(aliases = "k",throwable = true)
    public FunctionPrinter key;

    public ActStatic(CutsceneManager pl) {
        super(pl);
        if (manager == null) {
            ConditionBuilder.LIVING_ENTITY.BOOL.addFunction("static", new CheckableFunction<LivingEntity, Boolean>() {
                @Override
                public boolean check(JsonArray array) {
                    return array.size() > 0;
                }

                @Override
                public Boolean apply(LivingEntity entity, JsonArray array) {
                    return STATIC_MAP.containsKey(array.get(0).getAsString());
                }
            });
            manager = pl.register(new Listener() {
                @EventHandler
                public void death(PlayerDeathEvent e) {
                    remove(e.getEntity());
                }
                @EventHandler
                public void quit(PlayerQuitEvent e) {
                    remove(e.getPlayer());
                }
                @EventHandler
                public void teleport(PlayerTeleportEvent e) {
                    if (!ActSlate.SLATE_TOGGLE.contains(e.getPlayer())) remove(e.getPlayer());
                }

                private void remove(Player player) {
                    STATIC_MAP.forEach((k,v) -> {
                        if (player.equals(v)) STATIC_MAP.remove(k);
                    });
                }
            });
        }
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Player p = (Player) entity;
            String k = key.print(p);
            if (!STATIC_MAP.containsKey(k)) STATIC_MAP.put(k,p);
            else STATIC_MAP.remove(k);
        }
    }
}
