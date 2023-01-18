package kor.toxicity.cutscenemaker.actions.mechanics;

import com.google.gson.JsonArray;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.RepeatableAction;
import kor.toxicity.cutscenemaker.util.blockanims.AnimationPacket;
import kor.toxicity.cutscenemaker.util.blockanims.BlockAnimation;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class ActAnimation extends RepeatableAction {

    @DataField(aliases = "l",throwable = true)
    public JsonArray list;
    @DataField(aliases = "b")
    public boolean back = false;

    private final Map<String, BlockAnimation> animationMap;
    private final List<Consumer<Player>> packets = new ArrayList<>();

    public ActAnimation(CutsceneManager pl) {
        super(pl);
        animationMap = pl.getAnimationMap();
    }

    @Override
    public void initialize() {
        super.initialize();
        List<BlockAnimation> animations = new ArrayList<>();
        list.forEach(e -> {
            String name = e.getAsString();
            BlockAnimation animation = animationMap.get(name);
            if (animation != null) {
                int size = animations.size();
                if (size > 0) {
                    AnimationPacket beforeAir = animations.get(size - 1).toAirPacket();
                    AnimationPacket afterChange = animation.toPacket();
                    packets.add(p -> {
                        beforeAir.send(p);
                        afterChange.send(p);
                    });
                } else {
                    AnimationPacket packet = animation.toPacket();
                    packets.add(packet::send);
                }
                animations.add(animation);
            } else {
                CutsceneMaker.warn("The block animation \"" + name + "\" doesn't exist!");
            }
        });
        animations.clear();
        if (packets.size() == 0) throw new RuntimeException("There is no block animation in this action!");
        if (back) packets.add(packets.get(0));
        ticks = packets.size();
        if (interval < 1) interval = 20;
    }

    private final Map<Player,Integer> playerMap = new WeakHashMap<>();
    @Override
    protected void initialize(LivingEntity entity) {
        if (entity instanceof Player) playerMap.put((Player) entity,0);
    }

    @Override
    protected void update(LivingEntity entity) {
        if (entity instanceof Player) {
            Player p = (Player) entity;
            int i = playerMap.get(p);
            packets.get(Math.min(i,packets.size() - 1)).accept(p);
            playerMap.put(p,i + 1);
        }
    }

    @Override
    protected void end(LivingEntity end) {
        if (end instanceof Player) {
            playerMap.remove((Player) end);
        }
    }
}
