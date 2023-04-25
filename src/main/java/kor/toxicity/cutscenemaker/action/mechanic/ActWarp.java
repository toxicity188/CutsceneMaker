package kor.toxicity.cutscenemaker.action.mechanic;

import com.google.gson.JsonArray;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ActWarp extends CutsceneAction {

    @DataField(aliases = {"loc","l"},throwable = true)
    public JsonArray location;

    private Function<LivingEntity,Location> function;
    private final CutsceneManager manager;
    public ActWarp(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        List<String> loc = new ArrayList<>();
        location.forEach(j -> {
            if (j.isJsonPrimitive()) loc.add(j.getAsString());
        });
        List<Location> loc2 = loc.stream().map(s -> {
            Location loc3 = manager.getLocations().getValue(s);
            if (loc3 == null) CutsceneMaker.warn("unable to find a location named \"" + s + "\"");
            return loc3;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        function = p -> {
            int rand1 = ThreadLocalRandom.current().nextInt(0,loc.size());
            int rand2 = ThreadLocalRandom.current().nextInt(0,loc2.size());
            if (p instanceof Player) {
                return LocationStudio.getPlayerRecord((Player) p).map(r -> r.getLocation(loc.get(rand1))).orElse(loc2.get(rand2));
            } return loc2.get(rand2);
        };
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (function != null) entity.teleport(function.apply(entity));
    }
}
