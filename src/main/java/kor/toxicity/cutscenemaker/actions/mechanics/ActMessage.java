package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.DataField;
import kor.toxicity.cutscenemaker.actions.RepeatableAction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActMessage extends RepeatableAction {

    @DataField(aliases = "m")
    public String message = "<none>";
    @DataField(aliases = "st")
    public String subtitle = "";
    @DataField(aliases = {"r","rand"})
    public boolean random = false;
    @DataField()
    public String type = "message";

    @DataField(aliases = "fi")
    public int fadein = 10;
    @DataField(aliases = "s")
    public int stay = 20;
    @DataField(aliases = "fo")
    public int fadeout = 10;


    private String[] m;
    private String[] st;
    private BiConsumer<Entity,Integer> act;
    private final Map<Entity,Integer> id;

    public ActMessage(CutsceneManager pl) {
        super(pl);
        id = new HashMap<>();
    }

    @Override
    public void initialize() {
        a(message,s -> m = s);
        a(subtitle,s -> st = s);

        switch (type) {
            default:
            case "message":
                act = (e,i) -> e.sendMessage(b(e,m[i]));
                break;
            case "title":
                act = (e,i) -> {
                    if (e instanceof Player) ((Player) e).sendTitle(b(e,m[Math.min(m.length-1,i)]), b(e,st[Math.min(st.length-1,i)]), fadein, stay, fadeout);
                };
                break;
        }
    }
    private void a(String s, Consumer<String[]> c) {
        if (s == null) return;
        String t = s.replaceAll("&","§");
        c.accept(s.contains("/") ? t.split("/") : new String[] {t});
    }
    private String b(Entity e, String a) {
        return a.replaceAll("<name>",e.getName());
    }

    @Override
    protected void initialize(LivingEntity entity) {
        id.put(entity,0);
    }

    @Override
    protected void update(LivingEntity entity) {
        act.accept(entity,(random) ? ThreadLocalRandom.current().nextInt(0,m.length) : Math.min(m.length - 1,id.get(entity)));
        id.put(entity,id.get(entity) + 1);
    }

    @Override
    protected void end(LivingEntity end) {
        id.remove(end);
    }
}
